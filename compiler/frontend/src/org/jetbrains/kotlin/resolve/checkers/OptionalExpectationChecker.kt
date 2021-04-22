/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil

object OptionalExpectationChecker {
    fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, trace: BindingTrace) {
        val isExpect = descriptor is MemberDescriptor && descriptor.isExpect
        if (isExpect) {
            if (DescriptorUtils.isAnnotationClass(descriptor) && descriptor.containingDeclaration !is PackageFragmentDescriptor) {
                getOptionalExpectationEntry(declaration, trace)?.let {
                    trace.report(Errors.NESTED_OPTIONAL_EXPECTATION.on(it))
                }
            }
        } else {
            getOptionalExpectationEntry(declaration, trace)?.let {
                trace.report(Errors.OPTIONAL_EXPECTATION_NOT_ON_EXPECTED.on(it))
            }
        }
    }

    private fun getOptionalExpectationEntry(declaration: KtDeclaration, trace: BindingTrace): KtAnnotationEntry? =
        declaration.annotationEntries.find { entry ->
            val annotationDescriptor = trace.get(BindingContext.ANNOTATION, entry)
            annotationDescriptor?.fqName == OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME
        }
}
