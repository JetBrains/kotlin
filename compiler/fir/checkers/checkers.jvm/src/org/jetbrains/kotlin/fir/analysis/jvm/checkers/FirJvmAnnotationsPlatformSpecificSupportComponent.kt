/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.AnnotationsPosition
import org.jetbrains.kotlin.fir.declarations.FirAnnotationsPlatformSpecificSupportComponent
import org.jetbrains.kotlin.fir.declarations.FirCompilerRequiredParameterDescription
import org.jetbrains.kotlin.fir.declarations.FirCraParameterKind
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.types.ConstantValueKind

object FirJvmAnnotationsPlatformSpecificSupportComponent : FirAnnotationsPlatformSpecificSupportComponent() {
    override val requiredAnnotationsWithArguments: CompilerRequiredParametersMap = buildMap {
        this[JvmStandardClassIds.Annotations.Java.Target] = [
            FirCompilerRequiredParameterDescription(
                name = StandardNames.DEFAULT_VALUE_PARAMETER,
                kind = FirCraParameterKind.EnumParameter(JvmStandardClassIds.Annotations.Java.ElementType, isVararg = true),
                position = null,
            )
        ]

        this[JvmStandardClassIds.Annotations.JvmName] = [
            FirCompilerRequiredParameterDescription(
                name = StandardNames.NAME,
                kind = FirCraParameterKind.LiteralParameter(ConstantValueKind.String),
                position = 0,
            )
        ]
    }

    override val requiredAnnotations: Set<ClassId> = requiredAnnotationsWithArguments.keys + setOf(
        JvmStandardClassIds.Annotations.Java.Deprecated,
        JvmStandardClassIds.Annotations.JvmRecord,
        JvmStandardClassIds.Annotations.JvmInline,
    )

    override val volatileAnnotations: Set<ClassId> = setOf(
        JvmStandardClassIds.Annotations.JvmVolatile,
    )

    override val repeatableAnnotations: Set<ClassId> = setOf(
        JvmStandardClassIds.Annotations.Java.Repeatable,
        JvmStandardClassIds.Annotations.JvmRepeatable,
    )

    override val jvmInlineAnnotationClassId: ClassId = JvmStandardClassIds.Annotations.JvmInline

    override val deprecationAnnotationsWithOverridesPropagation: Map<ClassId, Boolean> = mapOf(
        JvmStandardClassIds.Annotations.Java.Deprecated to false,
    )

    override fun extractBackingFieldAnnotationsFromProperty(
        property: FirProperty,
        session: FirSession,
        propertyAnnotations: List<FirAnnotation>,
        backingFieldAnnotations: List<FirAnnotation>,
    ): AnnotationsPosition? {
        if (propertyAnnotations.isEmpty() || property.backingField == null) return null

        val [newBackingFieldAnnotations, newPropertyAnnotations] = propertyAnnotations.partition {
            it.toAnnotationClassIdSafe(session) == JvmStandardClassIds.Annotations.Java.Deprecated
        }

        if (newBackingFieldAnnotations.isEmpty()) return null
        return AnnotationsPosition(
            propertyAnnotations = newPropertyAnnotations,
            backingFieldAnnotations = backingFieldAnnotations + newBackingFieldAnnotations,
        )
    }
}
