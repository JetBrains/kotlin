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

fun List<FirAnnotationCall>.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    javaTypeEnhancementState: JavaTypeEnhancementState
): NullabilityQualifierWithMigrationStatus? =
    this.firstNotNullOfOrNull { annotationCall ->
        annotationCall.extractNullability(annotationTypeQualifierResolver, javaTypeEnhancementState)
    }


fun FirAnnotationCall.extractNullability(
    annotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver,
    javaTypeEnhancementState: JavaTypeEnhancementState
): NullabilityQualifierWithMigrationStatus? {
    this.extractNullabilityFromKnownAnnotations(javaTypeEnhancementState)?.let { return it }

    val typeQualifierAnnotation =
        annotationTypeQualifierResolver.resolveTypeQualifierAnnotation(this)
            ?: return null

    val jsr305ReportLevel = annotationTypeQualifierResolver.resolveJsr305ReportLevel(this)
    if (jsr305ReportLevel.isIgnore) return null

    return typeQualifierAnnotation.extractNullabilityFromKnownAnnotations(javaTypeEnhancementState)?.copy(isForWarningOnly = jsr305ReportLevel.isWarning)
}

private fun FirAnnotationCall.extractNullabilityFromKnownAnnotations(javaTypeEnhancementState: JavaTypeEnhancementState): NullabilityQualifierWithMigrationStatus? {
    val annotationClassId = classId ?: return null
    val reportLevel = javaTypeEnhancementState.getReportLevelForAnnotation(annotationClassId.asSingleFqName())

    if (reportLevel == ReportLevel.IGNORE) return null

    return when (annotationClassId) {
        in NULLABLE_ANNOTATION_IDS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, reportLevel.isWarning)
        in NOT_NULL_ANNOTATION_IDS -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, reportLevel.isWarning)
        JSPECIFY_NULLABLE_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, reportLevel.isWarning)
        JSPECIFY_NULLNESS_UNKNOWN_ANNOTATION_ID ->
            NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY, reportLevel.isWarning)
        JAVAX_NONNULL_ANNOTATION_ID -> extractNullabilityTypeFromArgument()
        COMPATQUAL_NULLABLE_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE, reportLevel.isWarning)
        COMPATQUAL_NONNULL_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL, reportLevel.isWarning)
        ANDROIDX_RECENTLY_NON_NULL_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NOT_NULL,
            isForWarningOnly = reportLevel.isWarning
        )
        ANDROIDX_RECENTLY_NULLABLE_ANNOTATION_ID -> NullabilityQualifierWithMigrationStatus(
            NullabilityQualifier.NULLABLE,
            isForWarningOnly = reportLevel.isWarning
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
val NOT_NULL_ANNOTATION_IDS = NOT_NULL_ANNOTATIONS.map { ClassId.topLevel(it) }
val JAVAX_NONNULL_ANNOTATION_ID = ClassId.topLevel(JAVAX_NONNULL_ANNOTATION)
val JAVAX_CHECKFORNULL_ANNOTATION_ID = ClassId.topLevel(JAVAX_CHECKFORNULL_ANNOTATION)
private val COMPATQUAL_NULLABLE_ANNOTATION_ID = ClassId.topLevel(COMPATQUAL_NULLABLE_ANNOTATION)
val COMPATQUAL_NONNULL_ANNOTATION_ID = ClassId.topLevel(COMPATQUAL_NONNULL_ANNOTATION)
val ANDROIDX_RECENTLY_NON_NULL_ANNOTATION_ID = ClassId.topLevel(ANDROIDX_RECENTLY_NON_NULL_ANNOTATION)
private val ANDROIDX_RECENTLY_NULLABLE_ANNOTATION_ID = ClassId.topLevel(ANDROIDX_RECENTLY_NULLABLE_ANNOTATION)
private val JSPECIFY_NULLABLE_ANNOTATION_ID = ClassId.topLevel(JSPECIFY_NULLABLE)
private val JSPECIFY_NULLNESS_UNKNOWN_ANNOTATION_ID = ClassId.topLevel(JSPECIFY_NULLNESS_UNKNOWN)