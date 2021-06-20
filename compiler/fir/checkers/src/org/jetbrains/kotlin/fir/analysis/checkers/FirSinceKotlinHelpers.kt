/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.SINCE_KOTLIN_FQ_NAME
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

fun FirAnnotatedDeclaration<*>.checkSinceKotlinVersionAccessibility(context: CheckerContext): FirSinceKotlinAccessibility {
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

private fun FirAnnotatedDeclaration<*>.getOwnSinceKotlinVersion(session: FirSession): FirSinceKotlinValue? {
    var result: FirSinceKotlinValue? = null

    // TODO: use-site targeted annotations
    fun FirAnnotatedDeclaration<*>.consider() {
        val sinceKotlinSingleArgument = getAnnotationByFqName(SINCE_KOTLIN_FQ_NAME)?.arguments?.singleOrNull()
        val apiVersion = ((sinceKotlinSingleArgument as? FirConstExpression<*>)?.value as? String)?.let(ApiVersion.Companion::parse)
        if (apiVersion != null) {
            // TODO: combine wasExperimentalMarkerClasses in case of several associated declarations with the same maximal API version
            if (result == null || apiVersion > result!!.apiVersion) {
                result = FirSinceKotlinValue(apiVersion, loadWasExperimentalMarkerClasses())
            }
        }
    }

    this.consider()
    if (this is FirConstructor) {
        val classId = symbol.callableId.classId
        if (classId != null) {
            val classSymbol = session.symbolProvider.getClassLikeSymbolByFqName(classId)
            classSymbol?.fir?.consider()
        }
    }

    if (this is FirTypeAlias) {
        (this.expandedTypeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir?.consider()
    }

    return result
}

private fun FirAnnotatedDeclaration<*>.loadWasExperimentalMarkerClasses(): List<FirRegularClassSymbol> {
    val wasExperimental = getAnnotationByFqName(OptInNames.WAS_EXPERIMENTAL_FQ_NAME) ?: return emptyList()
    val annotationClasses = wasExperimental.findArgumentByName(OptInNames.WAS_EXPERIMENTAL_ANNOTATION_CLASS) ?: return emptyList()
    return annotationClasses.extractClassesFromArgument()
}

