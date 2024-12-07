/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

object VolatileAnnotationChecker : DeclarationChecker {
    private val JVM_VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.jvm.Volatile")
    private val CONCURRENT_VOLATILE_ANNOTATION_FQ_NAME = FqName("kotlin.concurrent.Volatile")

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor) return

        val fieldAnnotation = descriptor.backingField?.findVolatileAnnotation()
        if (fieldAnnotation != null && !descriptor.isVar) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(fieldAnnotation) ?: return
            context.trace.report(Errors.VOLATILE_ON_VALUE.on(annotationEntry))
        }

        val delegateAnnotation = descriptor.delegateField?.findVolatileAnnotation()
        if (delegateAnnotation != null) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(delegateAnnotation) ?: return
            context.trace.report(Errors.VOLATILE_ON_DELEGATE.on(annotationEntry))
        }
    }

    private fun FieldDescriptor.findVolatileAnnotation(): AnnotationDescriptor? {
        return annotations.firstOrNull { it.fqName == JVM_VOLATILE_ANNOTATION_FQ_NAME || it.fqName == CONCURRENT_VOLATILE_ANNOTATION_FQ_NAME }
    }
}
