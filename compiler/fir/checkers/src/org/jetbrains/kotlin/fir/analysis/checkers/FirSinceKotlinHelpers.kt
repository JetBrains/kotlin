/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.checkers.OptInNames

sealed class FirSinceKotlinAccessibility {
    object Accessible : FirSinceKotlinAccessibility()

    data class NotAccessibleButWasExperimental(
        val version: ApiVersion,
        val markerClasses: List<FirRegularClassSymbol>
    ) : FirSinceKotlinAccessibility()

    data class NotAccessible(
        val version: ApiVersion
    ) : FirSinceKotlinAccessibility()
}

private data class FirSinceKotlinValue(
    val apiVersion: ApiVersion,
    val wasExperimentalMarkerClasses: List<FirRegularClassSymbol>
)

fun FirDeclaration.checkSinceKotlinVersionAccessibility(context: CheckerContext): FirSinceKotlinAccessibility {
    val value = getOwnSinceKotlinVersion(context.session)
    val version = value?.apiVersion
    val languageVersionSettings = context.session.languageVersionSettings

    // Allow access in the following cases:
    // 1) There's no @SinceKotlin annotation for this descriptor
    // 2) There's a @SinceKotlin annotation but its value is some unrecognizable nonsense
    // 3) The value as a version is not greater than our API version
    if (version == null || version <= languageVersionSettings.apiVersion) return FirSinceKotlinAccessibility.Accessible

    val wasExperimentalFqNames = value.wasExperimentalMarkerClasses
    if (wasExperimentalFqNames.isNotEmpty()) {
        return FirSinceKotlinAccessibility.NotAccessibleButWasExperimental(version, wasExperimentalFqNames)
    }

    return FirSinceKotlinAccessibility.NotAccessible(version)
}

private fun FirDeclaration.getOwnSinceKotlinVersion(session: FirSession): FirSinceKotlinValue? {
    val sinceKotlinSingleArgument = getAnnotationByClassId(StandardClassIds.Annotations.SinceKotlin, session)?.findArgumentByName(
        StandardClassIds.Annotations.ParameterNames.sinceKotlinVersion
    )
    val apiVersion = ((sinceKotlinSingleArgument as? FirLiteralExpression<*>)?.value as? String)?.let(ApiVersion.Companion::parse)
    return if (apiVersion != null) {
        FirSinceKotlinValue(apiVersion, loadWasExperimentalMarkerClasses(session))
    } else null
}

private fun FirDeclaration.loadWasExperimentalMarkerClasses(session: FirSession): List<FirRegularClassSymbol> {
    val wasExperimental = getAnnotationByClassId(OptInNames.WAS_EXPERIMENTAL_CLASS_ID, session) ?: return emptyList()
    val annotationClasses = wasExperimental.findArgumentByName(OptInNames.WAS_EXPERIMENTAL_ANNOTATION_CLASS) ?: return emptyList()
    return annotationClasses.extractClassesFromArgument(session)
}

