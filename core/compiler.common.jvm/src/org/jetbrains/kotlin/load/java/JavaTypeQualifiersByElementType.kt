/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.typeEnhancement.JavaTypeQualifiers
import org.jetbrains.kotlin.load.java.typeEnhancement.NullabilityQualifierWithMigrationStatus
import java.util.*

typealias QualifierByApplicabilityType = EnumMap<AnnotationQualifierApplicabilityType, NullabilityQualifierWithMigrationStatus?>

class JavaTypeQualifiersByElementType(val nullabilityQualifiers: QualifierByApplicabilityType) {
    operator fun get(applicabilityType: AnnotationQualifierApplicabilityType?): JavaTypeQualifiers? {
        val nullabilityQualifierWithMigrationStatus = nullabilityQualifiers[applicabilityType] ?: return null

        return JavaTypeQualifiers(
            nullabilityQualifierWithMigrationStatus.qualifier, null,
            isNotNullTypeParameter = false,
            isNullabilityQualifierForWarning = nullabilityQualifierWithMigrationStatus.isForWarningOnly
        )
    }
}
