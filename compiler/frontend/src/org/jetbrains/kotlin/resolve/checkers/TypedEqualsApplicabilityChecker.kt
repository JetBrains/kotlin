/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.isSuitableSignatureForTypedEquals
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

object TypedEqualsApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val annotation = descriptor.annotations.findAnnotation(StandardClassIds.Annotations.TypedEquals.asSingleFqName()) ?: return
        val functionDescriptor = context.trace.bindingContext.get(BindingContext.FUNCTION, declaration) ?: return
        if (!functionDescriptor.isSuitableSignatureForTypedEquals()) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
            context.trace.report(Errors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION.on(annotationEntry))
        }
    }
}