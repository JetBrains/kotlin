/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.AnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATIONS
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.platform.PlatformSpecificDiagnosticComponents

class JvmDiagnosticComponents(
    private val typeQualifierResolver: AnnotationTypeQualifierResolver
) : PlatformSpecificDiagnosticComponents {
    override fun isNullabilityAnnotation(
        annotationDescriptor: AnnotationDescriptor,
        containingDeclaration: DeclarationDescriptor
    ): Boolean {
        if (containingDeclaration !is JavaCallableMemberDescriptor) {
            return false
        }
        return annotationDescriptor.fqName?.let { it in NULLABILITY_ANNOTATIONS } == true
                || typeQualifierResolver.resolveTypeQualifierAnnotation(annotationDescriptor) != null
    }
}
