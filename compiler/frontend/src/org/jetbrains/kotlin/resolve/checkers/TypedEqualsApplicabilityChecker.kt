/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.util.OperatorNameConventions

object TypedEqualsApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val annotation = descriptor.annotations.findAnnotation(StandardClassIds.Annotations.TypedEquals.asSingleFqName()) ?: return
        val functionDescriptor = context.trace.bindingContext.get(BindingContext.FUNCTION, declaration) ?: return
        if (!functionDescriptor.hasSuitableSignatureForTypedEquals) {
            val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
            context.trace.report(Errors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION.on(annotationEntry))
        }
    }

    private val FunctionDescriptor.hasSuitableSignatureForTypedEquals: Boolean
        get() {
            val valueClassStarProjection =
                (containingDeclaration as? ClassDescriptor)?.takeIf { it.isValueClass() }?.defaultType?.replaceArgumentsWithStarProjections()
                    ?: return false
            val returnType = returnType ?: return false
            return name == OperatorNameConventions.EQUALS
                    && (returnType.isBoolean() || returnType.isNothing())
                    && valueParameters.size == 1 && valueParameters[0].type == valueClassStarProjection
                    && contextReceiverParameters.isEmpty() && extensionReceiverParameter == null
        }
}