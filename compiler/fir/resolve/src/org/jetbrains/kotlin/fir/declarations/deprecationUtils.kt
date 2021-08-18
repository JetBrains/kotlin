/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.builtins.StandardNames
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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val JAVA_DEPRECATED = FqName("java.lang.Deprecated")
private val HIDDEN_SINCE_NAME = Name.identifier("hiddenSince")
private val ERROR_SINCE_NAME = Name.identifier("errorSince")
private val WARNING_SINCE_NAME = Name.identifier("warningSince")
private val MESSAGE_NAME = Name.identifier("message")
private val LEVEL_NAME = Name.identifier("level")

private val JAVA_ORIGINS = setOf(FirDeclarationOrigin.Java, FirDeclarationOrigin.Enhancement)

fun FirBasedSymbol<*>.getDeprecation(callSite: FirElement?): Deprecation? {
    val deprecationInfos = mutableListOf<Deprecation>()
    when (this) {
        is FirPropertySymbol ->
            if (callSite is FirVariableAssignment) {
                deprecationInfos.addIfNotNull(
                    getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_SETTER, AnnotationUseSiteTarget.PROPERTY)
                )
            } else {
                deprecationInfos.addIfNotNull(
                    getDeprecationForCallSite(AnnotationUseSiteTarget.PROPERTY_GETTER, AnnotationUseSiteTarget.PROPERTY)
                )
            }
        else -> deprecationInfos.addIfNotNull(getDeprecationForCallSite())
    }

    return deprecationInfos.firstOrNull()
}

fun FirAnnotationContainer.getDeprecationInfos(currentVersion: ApiVersion): DeprecationsPerUseSite {
    val deprecationByUseSite = mutableMapOf<AnnotationUseSiteTarget?, Deprecation>()
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
    val perUseSite = buildMap<AnnotationUseSiteTarget, Deprecation> {
        setter?.getDeprecationInfos(currentVersion)?.all?.let { put(AnnotationUseSiteTarget.PROPERTY_SETTER, it) }
        getter?.getDeprecationInfos(currentVersion)?.all?.let { put(AnnotationUseSiteTarget.PROPERTY_GETTER, it) }
    }
    return if (perUseSite.isEmpty()) EmptyDeprecationsPerUseSite else DeprecationsPerUseSite(null, perUseSite)
}

fun List<FirAnnotationCall>.getDeprecationInfosFromAnnotations(currentVersion: ApiVersion, fromJava: Boolean): DeprecationsPerUseSite {
    val deprecationByUseSite = extractDeprecationInfoPerUseSite(currentVersion, fromJava).toMap()
    return DeprecationsPerUseSite.fromMap(deprecationByUseSite)
}

private fun FirBasedSymbol<*>.getDeprecationForCallSite(
    vararg sites: AnnotationUseSiteTarget
): Deprecation? {
    val deprecations = when (this) {
        is FirCallableSymbol<*> -> deprecation
        is FirClassLikeSymbol<*> -> deprecation
        else -> null
    }
    return (deprecations ?: EmptyDeprecationsPerUseSite).forUseSite(*sites)
}

private fun FirAnnotationCall.getVersionFromArgument(name: Name): ApiVersion? =
    getStringArgument(name)?.let { ApiVersion.parse(it) }

private fun FirAnnotationCall.getDeprecationLevel(): DeprecationLevelValue? {
    //take last because Annotation might be not resolved yet and arguments passed without explicit names
    val arg = findArgumentByName(LEVEL_NAME) ?: arguments.lastOrNull()
    return arg?.let { argument ->
        val targetExpression = argument as? FirQualifiedAccessExpression ?: return null
        val targetName = (targetExpression.calleeReference as? FirNamedReference)?.name?.asString() ?: return null
        DeprecationLevelValue.values().find { it.name == targetName }
    }
}

private fun List<FirAnnotationCall>.extractDeprecationInfoPerUseSite(
    currentVersion: ApiVersion,
    fromJava: Boolean
): List<Pair<AnnotationUseSiteTarget?, Deprecation>> {
    val annotations = getAnnotationsByFqName(StandardNames.FqNames.deprecated).map { it to false } +
            getAnnotationsByFqName(JAVA_DEPRECATED).map { it to true }
    return annotations.mapNotNull { (deprecated, fromJavaAnnotation) ->
        val deprecationLevel = deprecated.getDeprecationLevel() ?: DeprecationLevelValue.WARNING
        val deprecatedSinceKotlin = getAnnotationsByFqName(StandardNames.FqNames.deprecatedSinceKotlin).firstOrNull()

        fun levelApplied(name: Name, level: DeprecationLevelValue): DeprecationLevelValue? {
            deprecatedSinceKotlin?.getVersionFromArgument(name)?.takeIf { it <= currentVersion }?.let { return level }
            return level.takeIf { deprecatedSinceKotlin == null && level == deprecationLevel }
        }

        val appliedLevel = (levelApplied(HIDDEN_SINCE_NAME, DeprecationLevelValue.HIDDEN)
            ?: levelApplied(ERROR_SINCE_NAME, DeprecationLevelValue.ERROR)
            ?: levelApplied(WARNING_SINCE_NAME, DeprecationLevelValue.WARNING))

        appliedLevel?.let {
            val inheritable = !fromJavaAnnotation && !fromJava
            deprecated.useSiteTarget to Deprecation(it, inheritable, deprecated.getStringArgument(MESSAGE_NAME))
        }
    }
}

