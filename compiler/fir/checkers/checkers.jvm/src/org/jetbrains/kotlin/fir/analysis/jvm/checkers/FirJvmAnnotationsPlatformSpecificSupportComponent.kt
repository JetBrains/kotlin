/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJvmAnnotationsPlatformSpecificSupportComponent : FirAnnotationsPlatformSpecificSupportComponent() {
    override val requiredAnnotationsWithArguments: Set<ClassId> = setOf(
        JvmStandardClassIds.Annotations.Java.Target,
        JvmStandardClassIds.Annotations.JvmName,
    )

    override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments + setOf(
        JvmStandardClassIds.Annotations.Java.Deprecated,
        JvmStandardClassIds.Annotations.JvmRecord,
    )

    override val volatileAnnotations: Set<ClassId> = setOf(
        JvmStandardClassIds.Annotations.JvmVolatile,
    )

    override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
        JvmStandardClassIds.Annotations.Java.Deprecated to false,
    )

    override fun symbolContainsRepeatableAnnotation(symbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
        return symbol.hasAnnotationWithClassId(JvmStandardClassIds.Annotations.Java.Repeatable, session) ||
                symbol.hasAnnotationWithClassId(JvmStandardClassIds.Annotations.JvmRepeatable, session)
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
