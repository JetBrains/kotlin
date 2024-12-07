/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.metadata.deserialization.VersionRequirement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinErrorSince
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinHiddenSince
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinWarningSince
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.isFulfilled
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

class DeprecationAnnotationInfoPerUseSiteStorage(val storage: Map<AnnotationUseSiteTarget?, List<DeprecationInfoProvider>>) {
    fun toDeprecationsProvider(firCachesFactory: FirCachesFactory): DeprecationsProvider {
        if (storage.isEmpty()) {
            return EmptyDeprecationsProvider
        }
        @Suppress("UNCHECKED_CAST")
        val specificCallSite = storage.filterKeys { it != null } as Map<AnnotationUseSiteTarget, List<DeprecationInfoProvider>>
        return DeprecationsProviderImpl(
            firCachesFactory,
            storage[null],
            specificCallSite.takeIf { it.isNotEmpty() }
        )
    }

}

class DeprecationAnnotationInfoPerUseSiteStorageBuilder {
    private val storage = mutableMapOf<AnnotationUseSiteTarget?, MutableList<DeprecationInfoProvider>>()

    fun add(useSite: AnnotationUseSiteTarget?, info: DeprecationInfoProvider) {
        storage.getOrPut(useSite) { mutableListOf() }.add(info)
    }

    fun add(useSite: AnnotationUseSiteTarget?, infos: Iterable<DeprecationInfoProvider>) {
        storage.getOrPut(useSite) { mutableListOf() }.addAll(infos)
    }

    fun add(other: DeprecationAnnotationInfoPerUseSiteStorage) {
        other.storage.forEach { (useSite, info) ->
            add(useSite, info)
        }
    }

    fun build(): DeprecationAnnotationInfoPerUseSiteStorage {
        return DeprecationAnnotationInfoPerUseSiteStorage(storage)
    }
}

inline fun buildDeprecationAnnotationInfoPerUseSiteStorage(builder: DeprecationAnnotationInfoPerUseSiteStorageBuilder.() -> Unit)
        : DeprecationAnnotationInfoPerUseSiteStorage {
    return DeprecationAnnotationInfoPerUseSiteStorageBuilder().apply(builder).build()
}

private fun FirBasedSymbol<*>.getUseSitesForCallSite(callSite: FirElement?): Array<AnnotationUseSiteTarget> {
    return when (this) {
        is FirPropertySymbol -> when (callSite) {
            is FirVariableAssignment -> arrayOf(AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
            is FirPropertyAccessExpression -> arrayOf(AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)
            else -> arrayOf(AnnotationUseSiteTarget.PROPERTY)
        }
        else -> arrayOf()
    }
}

/**
 * Returns deprecation that is declared on the
 * corresponding declaration.
 */
fun FirBasedSymbol<*>.getOwnDeprecation(session: FirSession, callSite: FirElement?): FirDeprecationInfo? {
    return getOwnDeprecationForCallSite(session, *getUseSitesForCallSite(callSite))
}

/**
 * Returns deprecation that is declared on
 * the corresponding declaration directly
 * or, in case of a typealias, on any of
 * its expansions.
 */
fun FirBasedSymbol<*>.getDeprecation(session: FirSession, callSite: FirElement?): FirDeprecationInfo? {
    return getDeprecationForCallSite(session, *getUseSitesForCallSite(callSite))
}

fun FirAnnotationContainer.getDeprecationsProvider(session: FirSession): DeprecationsProvider {
    return extractDeprecationInfoPerUseSite(session).toDeprecationsProvider(session.firCachesFactory)
}

fun FirAnnotationContainer.extractDeprecationInfoPerUseSite(
    session: FirSession,
    customAnnotations: List<FirAnnotation>? = annotations,
    getterAnnotations: List<FirAnnotation>? = null,
    setterAnnotations: List<FirAnnotation>? = null,
): DeprecationAnnotationInfoPerUseSiteStorage {
    var fromJava = false
    var versionRequirements: List<VersionRequirement>? = null
    if (this is FirDeclaration) {
        fromJava = this.isJavaOrEnhancement
        versionRequirements = this.versionRequirements
    }
    return buildDeprecationAnnotationInfoPerUseSiteStorage {
        add((customAnnotations ?: annotations).extractDeprecationAnnotationInfoPerUseSite(fromJava, session, versionRequirements))
        if (this@extractDeprecationInfoPerUseSite is FirProperty) {
            add(
                getDeprecationsAnnotationInfoByUseSiteFromAccessors(
                    session = session,
                    getter = getter,
                    getterAnnotations = getterAnnotations,
                    setter = setter,
                    setterAnnotations = setterAnnotations,
                )
            )
        }
    }
}

fun getDeprecationsProviderFromAccessors(
    session: FirSession,
    getter: FirFunction?,
    setter: FirFunction?
): DeprecationsProvider = getDeprecationsAnnotationInfoByUseSiteFromAccessors(
    session = session,
    getter = getter,
    setter = setter,
).toDeprecationsProvider(session.firCachesFactory)

fun getDeprecationsAnnotationInfoByUseSiteFromAccessors(
    session: FirSession,
    getter: FirFunction?,
    getterAnnotations: List<FirAnnotation>? = getter?.annotations,
    setter: FirFunction?,
    setterAnnotations: List<FirAnnotation>? = setter?.annotations,
): DeprecationAnnotationInfoPerUseSiteStorage = buildDeprecationAnnotationInfoPerUseSiteStorage {
    val setterDeprecations = setter?.extractDeprecationInfoPerUseSite(session, customAnnotations = setterAnnotations)
    setterDeprecations?.storage?.forEach { (useSite, infos) ->
        if (useSite == null) {
            add(AnnotationUseSiteTarget.PROPERTY_SETTER, infos)
        } else {
            add(useSite, infos)
        }
    }

    val getterDeprecations = getter?.extractDeprecationInfoPerUseSite(session, customAnnotations = getterAnnotations)
    getterDeprecations?.storage?.forEach { (useSite, infos) ->
        if (useSite == null) {
            add(AnnotationUseSiteTarget.PROPERTY_GETTER, infos)
        } else {
            add(useSite, infos)
        }
    }
}

fun List<FirAnnotation>.getDeprecationsProviderFromAnnotations(
    session: FirSession,
    fromJava: Boolean,
    versionRequirements: List<VersionRequirement>? = null,
): DeprecationsProvider {
    val deprecationAnnotationByUseSite = extractDeprecationAnnotationInfoPerUseSite(fromJava, session, versionRequirements)
    return deprecationAnnotationByUseSite.toDeprecationsProvider(session.firCachesFactory)
}

/**
 * Returns deprecation that is declared on the
 * corresponding declaration.
 */
private fun FirBasedSymbol<*>.getOwnDeprecationForCallSite(
    session: FirSession,
    vararg sites: AnnotationUseSiteTarget
): FirDeprecationInfo? {
    val deprecations = when (this) {
        is FirCallableSymbol<*> -> getDeprecation(session.languageVersionSettings)
        is FirClassLikeSymbol<*> -> getOwnDeprecation(session.languageVersionSettings)
        else -> null
    }
    return (deprecations ?: EmptyDeprecationsPerUseSite).forUseSite(*sites)
}

/**
 * Returns deprecation that is declared on
 * the corresponding declaration directly
 * or, in case of a typealias, on any of
 * its expansions.
 */
fun FirBasedSymbol<*>.getDeprecationForCallSite(
    session: FirSession,
    vararg sites: AnnotationUseSiteTarget,
): FirDeprecationInfo? {
    return when (this) {
        !is FirTypeAliasSymbol -> getOwnDeprecationForCallSite(session, *sites)
        else -> {
            var worstDeprecationInfo = getOwnDeprecationForCallSite(session, *sites)
            val visited = mutableMapOf<ConeKotlinType, FirDeprecationInfo?>()

            resolvedExpandedTypeRef.coneType.forEachType {
                val deprecationInfo = visited.getOrPut(it) {
                    val symbol = it.toSymbol(session) ?: return@forEachType
                    symbol.getDeprecationForCallSite(session, *sites)
                } ?: return@forEachType

                val currentWorstDeprecation = worstDeprecationInfo

                if (currentWorstDeprecation == null || deprecationInfo > currentWorstDeprecation) {
                    worstDeprecationInfo = deprecationInfo
                }
            }

            worstDeprecationInfo
        }
    }
}

private fun FirAnnotation.getDeprecationLevel(): DeprecationLevelValue? {
    //take last because Annotation might be not resolved yet and arguments passed without explicit names
    val argument = if (resolved) {
        argumentMapping.mapping[ParameterNames.deprecatedLevel]
    } else {
        val call = this as? FirAnnotationCall ?: return null
        call.arguments
            .firstOrNull { it is FirNamedArgumentExpression && it.name == ParameterNames.deprecatedLevel }
            ?.unwrapArgument()
            ?: arguments.lastOrNull()
    } ?: return null

    val targetName = argument.extractEnumValueArgumentInfo()?.enumEntryName?.asString() ?: return null

    return DeprecationLevelValue.entries.find { it.name == targetName }
}

private fun List<FirAnnotation>.extractDeprecationAnnotationInfoPerUseSite(
    fromJava: Boolean,
    session: FirSession,
    versionRequirements: List<VersionRequirement>?,
): DeprecationAnnotationInfoPerUseSiteStorage {
    // NB: We can't expand typealiases (`toAnnotationClassId`), because it
    // requires `lookupTag.tySymbol()`, but we can have cycles in annotations.
    // See the commit message for an example.

    val annotations = session.annotationPlatformSupport.deprecationAnnotationsWithOverridesPropagation
        .flatMap { (classId, shouldPropagateToOverrides) ->
            this.filter {
                it.unexpandedClassId == classId
            }.map {
                it to shouldPropagateToOverrides
            }
        }

    return buildDeprecationAnnotationInfoPerUseSiteStorage {
        for ((deprecated, shouldPropagateToOverrides) in annotations) {
            if (deprecated.unexpandedClassId == StandardClassIds.Annotations.SinceKotlin) {
                val sinceKotlinSingleArgument = deprecated.findArgumentByName(ParameterNames.sinceKotlinVersion)
                val apiVersion = ((sinceKotlinSingleArgument as? FirLiteralExpression)?.value as? String)
                    ?.let(ApiVersion.Companion::parse) ?: continue
                val wasExperimental = this@extractDeprecationAnnotationInfoPerUseSite.any {
                    it.unexpandedClassId == StandardClassIds.Annotations.WasExperimental
                }
                if (!wasExperimental) {
                    add(deprecated.useSiteTarget, SinceKotlinProvider(apiVersion))
                }
            } else {
                val deprecationLevel = deprecated.getDeprecationLevel() ?: DeprecationLevelValue.WARNING
                val propagatesToOverride = shouldPropagateToOverrides && !fromJava
                val deprecatedSinceKotlin = this@extractDeprecationAnnotationInfoPerUseSite.firstOrNull {
                    it.unexpandedClassId == StandardClassIds.Annotations.DeprecatedSinceKotlin
                }
                val deprecatedInfo =
                    if (deprecatedSinceKotlin == null) {
                        SimpleDeprecatedProvider(deprecationLevel, propagatesToOverride, deprecated)
                    } else {
                        DeprecatedSinceKotlinProvider(
                            deprecatedSinceKotlin,
                            deprecated,
                            propagatesToOverride
                        )
                    }
                add(deprecated.useSiteTarget, deprecatedInfo)
            }
        }

        versionRequirements?.forEach {
            add(null, RequireKotlinProvider(it))
        }
    }
}

fun FirBasedSymbol<*>.isDeprecationLevelHidden(session: FirSession): Boolean =
    when (this) {
        is FirCallableSymbol<*> -> getDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN
        is FirClassLikeSymbol<*> -> getOwnDeprecation(session.languageVersionSettings)?.all?.deprecationLevel == DeprecationLevelValue.HIDDEN
        else -> false
    }

private object IsHiddenEverywhereBesideSuperCalls : FirDeclarationDataKey()

var FirCallableDeclaration.hiddenEverywhereBesideSuperCallsStatus: HiddenEverywhereBesideSuperCallsStatus? by FirDeclarationDataRegistry.data(
    IsHiddenEverywhereBesideSuperCalls
)

enum class HiddenEverywhereBesideSuperCallsStatus {
    HIDDEN, HIDDEN_IN_DECLARING_CLASS_ONLY, HIDDEN_FAKE,
}

private object IsHiddenToOvercomeSignatureClash : FirDeclarationDataKey()

var FirCallableDeclaration.isHiddenToOvercomeSignatureClash: Boolean? by FirDeclarationDataRegistry.data(
    IsHiddenToOvercomeSignatureClash
)

enum class CallToPotentiallyHiddenSymbolResult {
    Hidden, Visible, VisibleWithDeprecation,
}

/**
 * To check whether a symbol is visible and if it's deprecated, the method needs to be called for the symbol and all its
 * overridden symbols.
 * [isSuperCall] must be set to `true` when the receiver is `super`.
 * [isCallToOverride] must be set to `false` for the original symbol and to `true` for all its overridden symbols.
 *
 * Given the following hierarchy
 *
 * ```
 * public class A {
 *     public String getX() { return ""; }  // HIDDEN
 *     public String getY() { return ""; }  // HIDDEN_IN_DECLARING_CLASS_ONLY
 *     public String getZ() { return ""; }  // HIDDEN_FAKE
 * }
 *
 * class B extends A {
 *     @Override public String getX() { return super.getX(); }
 *     @Override public String getY() { return super.getY(); }
 *     @Override public String getZ() { return super.getZ(); }
 * }
 * ```
 *
 * the results will be as follows
 *
 * | Receiver \ Symbol | getX    | getY                   | getZ                   |
 * |-------------------|---------|------------------------|------------------------|
 * | A                 | Hidden  | Hidden                 | Hidden                 |
 * | super             | Visible | VisibleWithDeprecation | Hidden                 |
 * | B                 | Hidden  | VisibleWithDeprecation | VisibleWithDeprecation |
 *
 */
fun FirCallableSymbol<*>.hiddenStatusOfCall(isSuperCall: Boolean, isCallToOverride: Boolean): CallToPotentiallyHiddenSymbolResult {
    val fir = fir
    if (fir.isHiddenToOvercomeSignatureClash == true) {
        return CallToPotentiallyHiddenSymbolResult.Hidden
    }

    val status = fir.hiddenEverywhereBesideSuperCallsStatus ?: return CallToPotentiallyHiddenSymbolResult.Visible

    return when (status) {
        // If the declaration is HIDDEN, we don't need a deprecation on supercalls because what are we warning the user about?
        // The declaration can't get any more hidden.
        HiddenEverywhereBesideSuperCallsStatus.HIDDEN -> if (isSuperCall) CallToPotentiallyHiddenSymbolResult.Visible else CallToPotentiallyHiddenSymbolResult.Hidden
        // However, on HIDDEN_IN_DECLARING_CLASS_ONLY,
        // we report a deprecation warning on super calls because we might want to rename the method in the future
        // (getFirst -> first).
        HiddenEverywhereBesideSuperCallsStatus.HIDDEN_IN_DECLARING_CLASS_ONLY -> if (isSuperCall || isCallToOverride) CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation else CallToPotentiallyHiddenSymbolResult.Hidden
        // HIDDEN_FAKE is always hidden (even for super calls), unless overridden.
        HiddenEverywhereBesideSuperCallsStatus.HIDDEN_FAKE -> if (isCallToOverride) CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation else CallToPotentiallyHiddenSymbolResult.Hidden
    }
}

data class FutureApiDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    override val propagatesToOverrides: Boolean,
    val sinceVersion: ApiVersion,
) : FirDeprecationInfo() {
    override fun getMessage(session: FirSession): String? {
        return null
    }
}

data class RequireKotlinDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    val versionRequirement: VersionRequirement,
) : FirDeprecationInfo() {
    override fun getMessage(session: FirSession): String? {
        return versionRequirement.message
    }

    override val propagatesToOverrides: Boolean get() = false
}

class RequireKotlinProvider(private val versionRequirement: VersionRequirement) : DeprecationInfoProvider() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo? {
        return runUnless(versionRequirement.isFulfilled(languageVersionSettings)) {
            RequireKotlinDeprecationInfo(
                deprecationLevel = when (versionRequirement.level) {
                    DeprecationLevel.WARNING -> DeprecationLevelValue.WARNING
                    DeprecationLevel.ERROR -> DeprecationLevelValue.ERROR
                    DeprecationLevel.HIDDEN -> DeprecationLevelValue.HIDDEN
                },
                versionRequirement = versionRequirement
            )
        }
    }
}

class SinceKotlinProvider(private val sinceVersion: ApiVersion) : DeprecationInfoProvider() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo? {
        return runUnless(sinceVersion <= languageVersionSettings.apiVersion) {
            FutureApiDeprecationInfo(
                deprecationLevel = DeprecationLevelValue.HIDDEN,
                propagatesToOverrides = true,
                sinceVersion = sinceVersion,
            )
        }
    }
}

class SimpleDeprecatedProvider(
    private val level: DeprecationLevelValue,
    private val propagatesToOverride: Boolean,
    private val annotation: FirAnnotation,
) : DeprecationInfoProvider() {
    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo {
        return SimpleFirDeprecationInfo(level, propagatesToOverride, annotation)
    }
}

class DeprecatedSinceKotlinProvider(
    private val deprecatedSinceKotlinAnnotation: FirAnnotation,
    private val deprecatedAnnotation: FirAnnotation,
    private val propagatesToOverride: Boolean,
) : DeprecationInfoProvider() {
    private fun DeprecationLevelValue.takeIfVersionMatches(
        name: Name,
        index: Int,
        languageVersionSettings: LanguageVersionSettings,
    ): DeprecationLevelValue? {
        // We can't use getStringArgument because this can be called during TYPES phase when we don't have an argument mapping, yet.
        val argument = deprecatedSinceKotlinAnnotation.findArgumentByName(name, returnFirstWhenNotFound = false)
            ?: (deprecatedSinceKotlinAnnotation as? FirAnnotationCall)?.arguments?.elementAtOrNull(index)

        val version = ((argument as? FirLiteralExpression)?.value as? String)
            ?.let { ApiVersion.parse(it) }
            ?: return null

        return takeIf { version <= languageVersionSettings.apiVersion }
    }

    override fun computeDeprecationInfo(languageVersionSettings: LanguageVersionSettings): FirDeprecationInfo? {
        val appliedLevel = DeprecationLevelValue.HIDDEN.takeIfVersionMatches(deprecatedSinceKotlinHiddenSince, 2, languageVersionSettings)
            ?: DeprecationLevelValue.ERROR.takeIfVersionMatches(deprecatedSinceKotlinErrorSince, 1, languageVersionSettings)
            ?: DeprecationLevelValue.WARNING.takeIfVersionMatches(deprecatedSinceKotlinWarningSince, 0, languageVersionSettings)

        return appliedLevel?.let {
            SimpleFirDeprecationInfo(it, propagatesToOverride, deprecatedAnnotation)
        }
    }
}

class SimpleFirDeprecationInfo(
    override val deprecationLevel: DeprecationLevelValue,
    override val propagatesToOverrides: Boolean,
    private val annotation: FirAnnotation,
) : FirDeprecationInfo() {
    override fun getMessage(session: FirSession): String? {
        (annotation as? FirAnnotationCall)?.containingDeclarationSymbol?.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
        return annotation.getStringArgument(ParameterNames.deprecatedMessage, session)
    }

}
