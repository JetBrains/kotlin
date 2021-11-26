/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinErrorSince
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinHiddenSince
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.ParameterNames.deprecatedSinceKotlinWarningSince
import org.jetbrains.kotlin.resolve.deprecation.DeprecationInfo
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.deprecation.SimpleDeprecationInfo
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val JAVA_ORIGINS = setOf(FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement)

fun FirBasedSymbol<*>.getDeprecation(callSite: FirElement?): DeprecationInfo? {
    return when (this) {
        is FirPropertySymbol ->
            when (callSite) {
                is FirVariableAssignment ->
                    getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
                is FirPropertyAccessExpression ->
                    getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)
                else ->
                    getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY)
            }
        else ->
            getDeprecationForCallSite()
    }
}

fun FirAnnotationContainer.getDeprecationInfos(currentVersion: ApiVersion): DeprecationsPerUseSite {
    val deprecationByUseSite = mutableMapOf<AnnotationUseSiteTarget?, DeprecationInfo>()
    val fromJava = JAVA_ORIGINS.contains(this.safeAs<FirDeclaration>()?.origin)
    annotations.extractDeprecationInfoPerUseSite(currentVersion, fromJava).toMap(deprecationByUseSite)

    if (this is FirProperty) {
        getDeprecationsFromAccessors(getter, setter, currentVersion).bySpecificSite?.forEach { (k, v) -> deprecationByUseSite[k] = v }
    }

    return DeprecationsPerUseSite.fromMap(deprecationByUseSite)
}

@OptIn(ExperimentalStdlibApi::class)
fun getDeprecationsFromAccessors(
    getter: FirFunction?,
    setter: FirFunction?,
    currentVersion: ApiVersion
): DeprecationsPerUseSite {
    val perUseSite = buildMap<AnnotationUseSiteTarget, DeprecationInfo> {
        val setterDeprecations = setter?.getDeprecationInfos(currentVersion)
        setterDeprecations?.all?.let { put(AnnotationUseSiteTarget.PROPERTY_SETTER, it) }
        setterDeprecations?.bySpecificSite?.let { putAll(it) }
        val getterDeprecations = getter?.getDeprecationInfos(currentVersion)
        getterDeprecations?.all?.let { put(AnnotationUseSiteTarget.PROPERTY_GETTER, it) }
        getterDeprecations?.bySpecificSite?.let { putAll(it) }
    }
    return if (perUseSite.isEmpty()) EmptyDeprecationsPerUseSite else DeprecationsPerUseSite(null, perUseSite)
}

fun List<FirAnnotation>.getDeprecationInfosFromAnnotations(currentVersion: ApiVersion, fromJava: Boolean): DeprecationsPerUseSite {
    val deprecationByUseSite = extractDeprecationInfoPerUseSite(currentVersion, fromJava).toMap()
    return DeprecationsPerUseSite.fromMap(deprecationByUseSite)
}

fun FirBasedSymbol<*>.getDeprecationForCallSite(
    vararg sites: AnnotationUseSiteTarget
): DeprecationInfo? {
    val deprecations = when (this) {
        is FirCallableSymbol<*> -> deprecation
        is FirClassLikeSymbol<*> -> deprecation
        else -> null
    }
    return (deprecations ?: EmptyDeprecationsPerUseSite).forUseSite(*sites)
}

private fun FirAnnotation.getVersionFromArgument(name: Name): ApiVersion? =
    getStringArgument(name)?.let { ApiVersion.parse(it) }

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
    val targetExpression = argument as? FirQualifiedAccessExpression ?: return null
    val targetName = (targetExpression.calleeReference as? FirNamedReference)?.name?.asString() ?: return null
    return DeprecationLevelValue.values().find { it.name == targetName }
}

private fun List<FirAnnotation>.extractDeprecationInfoPerUseSite(
    currentVersion: ApiVersion,
    fromJava: Boolean
): List<Pair<AnnotationUseSiteTarget?, DeprecationInfo>> {
    val annotations = getAnnotationsByClassId(StandardClassIds.Annotations.Deprecated).map { it to false } +
            getAnnotationsByClassId(StandardClassIds.Annotations.Java.Deprecated).map { it to true }
    return annotations.mapNotNull { (deprecated, fromJavaAnnotation) ->
        val deprecationLevel = deprecated.getDeprecationLevel() ?: DeprecationLevelValue.WARNING
        val deprecatedSinceKotlin = getAnnotationsByClassId(StandardClassIds.Annotations.DeprecatedSinceKotlin).firstOrNull()

        fun levelApplied(name: Name, level: DeprecationLevelValue): DeprecationLevelValue? {
            deprecatedSinceKotlin?.getVersionFromArgument(name)?.takeIf { it <= currentVersion }?.let { return level }
            return level.takeIf { deprecatedSinceKotlin == null && level == deprecationLevel }
        }

        val appliedLevel = (levelApplied(deprecatedSinceKotlinHiddenSince, DeprecationLevelValue.HIDDEN)
            ?: levelApplied(deprecatedSinceKotlinErrorSince, DeprecationLevelValue.ERROR)
            ?: levelApplied(deprecatedSinceKotlinWarningSince, DeprecationLevelValue.WARNING))

        appliedLevel?.let {
            val inheritable = !fromJavaAnnotation && !fromJava
            deprecated.useSiteTarget to SimpleDeprecationInfo(
                it,
                inheritable,
                deprecated.getStringArgument(ParameterNames.deprecatedMessage)
            )
        }
    }
}

