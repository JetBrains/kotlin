/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

fun List<FirAnnotationCall>.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    jsr305State: Jsr305State
): NullabilityQualifierWithMigrationStatus? =
    this.firstNotNullResult { annotationCall ->
        annotationCall.extractNullability(annotationTypeQualifierResolver, jsr305State)
    }


fun FirAnnotationCall.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    jsr305State: Jsr305State
): NullabilityQualifierWithMigrationStatus? {
    this.extractNullabilityFromKnownAnnotations(jsr305State)?.let { return it }

    val typeQualifierAnnotation =
        annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(this)
            ?: return null

    val jsr305ReportLevel = annotationTypeQualifierResolver.resolveJsr305ReportLevel(this)
    if (jsr305ReportLevel.isIgnore) return null

    return typeQualifierAnnotation.extractNullabilityFromKnownAnnotations(jsr305State)?.copy(isForWarningOnly = jsr305ReportLevel.isWarning)
}

private fun FirAnnotationCall.extractNullabilityFromKnownAnnotations(jsr305State: Jsr305State): NullabilityQualifierWithMigrationStatus? {
    val annotationClassId = classId ?: return null

    return when {
        annotationClassId in NULLABLE_ANNOTATION_IDS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
        annotationClassId in NOT_NULL_ANNOTATION_IDS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
        annotationClassId == JAVAX_NONNULL_ANNOTATION_ID -> extractNullabilityTypeFromArgument()

        annotationClassId == COMPATQUAL_NULLABLE_ANNOTATION_ID && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)

        annotationClassId == COMPATQUAL_NONNULL_ANNOTATION_ID && jsr305State.enableCompatqualCheckerFrameworkAnnotations ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

        annotationClassId == ANDROIDX_RECENTLY_NON_NULL_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NOT_NULL,
            isForWarningOnly = true
        )

        annotationClassId == ANDROIDX_RECENTLY_NULLABLE_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NULLABLE,
            isForWarningOnly = true
        )
        else -> null
    }
}

private fun FirAnnotationCall.extractNullabilityTypeFromArgument(): NullabilityQualifierWithMigrationStatus? {
    val enumValue = this.arguments.firstOrNull()?.toResolvedCallableSymbol()?.callableId?.callableName
    // if no argument is specified, use default value: NOT_NULL
        ?: return NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)

    return when (enumValue.asString()) {
        "ALWAYS" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL)
        "MAYBE", "NEVER" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE)
        "UNKNOWN" -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY)
        else -> null
    }
}

private val NULLABLE_ANNOTATION_IDS = NULLABLE_ANNOTATIONS.map { ClassId.topLevel(it) }
private val NOT_NULL_ANNOTATION_IDS = NOT_NULL_ANNOTATIONS.map { ClassId.topLevel(it) }
private val JAVAX_NONNULL_ANNOTATION_ID = ClassId.topLevel(JAVAX_NONNULL_ANNOTATION)
private val COMPATQUAL_NULLABLE_ANNOTATION_ID = ClassId.topLevel(COMPATQUAL_NULLABLE_ANNOTATION)
private val COMPATQUAL_NONNULL_ANNOTATION_ID = ClassId.topLevel(COMPATQUAL_NONNULL_ANNOTATION)
private val ANDROIDX_RECENTLY_NON_NULL_ANNOTATION_ID = ClassId.topLevel(ANDROIDX_RECENTLY_NON_NULL_ANNOTATION)
private val ANDROIDX_RECENTLY_NULLABLE_ANNOTATION_ID = ClassId.topLevel(ANDROIDX_RECENTLY_NULLABLE_ANNOTATION)
