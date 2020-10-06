/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifier
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import org.jetbrains.kotlin.name.FqName

val TYPE_QUALIFIER_NICKNAME_FQNAME = FqName("javax.annotation.meta.TypeQualifierNickname")
val TYPE_QUALIFIER_FQNAME = FqName("javax.annotation.meta.TypeQualifier")
val TYPE_QUALIFIER_DEFAULT_FQNAME = FqName("javax.annotation.meta.TypeQualifierDefault")

val MIGRATION_ANNOTATION_FQNAME = FqName("kotlin.annotations.jvm.UnderMigration")

val BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS = mapOf(
    FqName("javax.annotation.ParametersAreNullableByDefault") to
            NullabilityQualifierWithApplicability(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NULLABLE),
                listOf(AnnotationQualifierApplicabilityType.VALUE_PARAMETER)
            ),
    FqName("javax.annotation.ParametersAreNonnullByDefault") to
            NullabilityQualifierWithApplicability(
                NullabilityQualifierWithMigrationStatus(NullabilityQualifier.NOT_NULL),
                listOf(AnnotationQualifierApplicabilityType.VALUE_PARAMETER)
            )
)

val BUILT_IN_TYPE_QUALIFIER_FQ_NAMES = setOf(JAVAX_NONNULL_ANNOTATION, JAVAX_CHECKFORNULL_ANNOTATION)
