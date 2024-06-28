/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmAnnotationsPlatformSpecificSupportComponent : FirAnnotationsPlatformSpecificSupportComponent() {
    override val requiredAnnotationsWithArguments: Set<ClassId> = setOf(
        StandardClassIds.Annotations.Deprecated,
        StandardClassIds.Annotations.Target,
        JvmStandardClassIds.Annotations.Java.Target,
        JvmStandardClassIds.Annotations.JvmName,
    )

    override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments + setOf(
        JvmStandardClassIds.Annotations.Java.Deprecated,
        StandardClassIds.Annotations.DeprecatedSinceKotlin,
        StandardClassIds.Annotations.SinceKotlin,
        StandardClassIds.Annotations.WasExperimental,
        JvmStandardClassIds.Annotations.JvmRecord,
    )

    override val volatileAnnotations: Set<ClassId> = setOf(
        StandardClassIds.Annotations.Volatile,
        JvmStandardClassIds.Annotations.JvmVolatile,
    )

    override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
        StandardClassIds.Annotations.Deprecated to true,
        JvmStandardClassIds.Annotations.Java.Deprecated to false,
        StandardClassIds.Annotations.SinceKotlin to true,
    )

    override fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
        if (symbol.getAnnotationByClassId(StandardClassIds.Annotations.Repeatable, session) != null) return true
        if (symbol.getAnnotationByClassId(JvmStandardClassIds.Annotations.Java.Repeatable, session) != null ||
            symbol.getAnnotationByClassId(JvmStandardClassIds.Annotations.JvmRepeatable, session) != null
        ) {
            return session.languageVersionSettings.supportsFeature(LanguageFeature.RepeatableAnnotations) ||
                    symbol.getAnnotationRetention(session) == AnnotationRetention.SOURCE && symbol.origin is FirDeclarationOrigin.Java
        }
        return false
    }

    override fun extractBackingFieldAnnotationsFromProperty(
        property: FirProperty,
        session: FirSession,
        propertyAnnotations: List<FirAnnotation>,
        backingFieldAnnotations: List<FirAnnotation>,
    ): AnnotationsPosition? {
        if (propertyAnnotations.isEmpty() || property.backingField == null) return null

        val (newBackingFieldAnnotations, newPropertyAnnotations) = propertyAnnotations.partition {
            it.toAnnotationClassIdSafe(session) == JvmStandardClassIds.Annotations.Java.Deprecated
        }

        if (newBackingFieldAnnotations.isEmpty()) return null
        return AnnotationsPosition(
            propertyAnnotations = newPropertyAnnotations,
            backingFieldAnnotations = backingFieldAnnotations + newBackingFieldAnnotations,
        )
    }
}
