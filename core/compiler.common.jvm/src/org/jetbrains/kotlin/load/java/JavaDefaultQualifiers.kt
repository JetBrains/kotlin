/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus

data class JavaDefaultQualifiers(
    val nullabilityQualifier: NullabilityQualifierWithMigrationStatus,
    val qualifierApplicabilityTypes: Collection<AnnotationQualifierApplicabilityType>,
    val definitelyNotNull: Boolean = nullabilityQualifier.qualifier == NullabilityQualifier.NOT_NULL
)

val APPLICABILITY_OF_JSPECIFY_DEFAULTS = listOf(
    AnnotationQualifierApplicabilityType.FIELD,
    AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE,
    AnnotationQualifierApplicabilityType.VALUE_PARAMETER,
    AnnotationQualifierApplicabilityType.TYPE_PARAMETER_BOUNDS,
    AnnotationQualifierApplicabilityType.TYPE_USE
)

val APPLICABILITY_OF_JAVAX_DEFAULTS = listOf(
    AnnotationQualifierApplicabilityType.VALUE_PARAMETER
)

val JSPECIFY_DEFAULT_ANNOTATIONS = mapOf(
    JSPECIFY_OLD_NULL_MARKED_ANNOTATION_FQ_NAME to
            JavaDefaultQualifiers(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                APPLICABILITY_OF_JSPECIFY_DEFAULTS,
                definitelyNotNull = false
            ),
    JSPECIFY_NULL_MARKED_ANNOTATION_FQ_NAME to
            JavaDefaultQualifiers(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                APPLICABILITY_OF_JSPECIFY_DEFAULTS,
                definitelyNotNull = false
            ),
    JSPECIFY_NULL_UNMARKED_ANNOTATION_FQ_NAME to
            JavaDefaultQualifiers(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.FORCE_FLEXIBILITY),
                APPLICABILITY_OF_JSPECIFY_DEFAULTS
            ),
)

val JAVAX_DEFAULT_ANNOTATIONS = mapOf(
    JAVAX_PARAMETERS_ARE_NONNULL_BY_DEFAULT_ANNOTATION_FQ_NAME to
            JavaDefaultQualifiers(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                APPLICABILITY_OF_JAVAX_DEFAULTS
            ),
    JAVAX_PARAMETERS_ARE_NULLABLE_BY_DEFAULT_ANNOTATION_FQ_NAME to
            JavaDefaultQualifiers(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE),
                APPLICABILITY_OF_JAVAX_DEFAULTS
            ),
)

val BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS = JSPECIFY_DEFAULT_ANNOTATIONS + JAVAX_DEFAULT_ANNOTATIONS
