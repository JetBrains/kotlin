/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithStarProjections
import org.jetbrains.kotlin.util.OperatorNameConventions

object TypedEqualsApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtFunction) return
        val typedEqualsAnnotation =
            descriptor.annotations.findAnnotation(StandardClassIds.Annotations.TypedEquals.asSingleFqName()) ?: return
        val functionDescriptor = context.trace.bindingContext.get(BindingContext.FUNCTION, declaration) ?: return
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(typedEqualsAnnotation) ?: return
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CustomEqualsInValueClasses)) {
            context.trace.report(
                Errors.UNSUPPORTED_FEATURE.on(
                    annotationEntry,
                    LanguageFeature.CustomEqualsInValueClasses to context.languageVersionSettings
                )
            )
            return
        }
        val parentClass = (functionDescriptor.containingDeclaration as? ClassDescriptor)?.takeIf { it.isValueClass() }
        if (parentClass == null) {
            context.trace.report(
                Errors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION.on(
                    annotationEntry,
                    "function must be a member of value class"
                )
            )
            return
        }
        if (!functionDescriptor.hasSuitableSignatureForTypedEquals(parentClass)) {
            context.trace.report(Errors.INAPPLICABLE_TYPED_EQUALS_ANNOTATION.on(annotationEntry, "unexpected signature"))
            return
        }
        if (functionDescriptor.valueParameters[0].hasDefaultValue()) {
            val parameter = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor.valueParameters[0]) as? KtParameter
                ?: error("Declaration not found for parameter: ${functionDescriptor.valueParameters[0]}")
            context.trace.report(Errors.DEFAULT_VALUE_NOT_ALLOWED_IN_TYPED_EQUALS.on(parameter))
        }
    }

    private fun FunctionDescriptor.hasSuitableSignatureForTypedEquals(parentClass: ClassDescriptor): Boolean {
        val returnType = returnType ?: return false
        return name == OperatorNameConventions.EQUALS
                && (returnType.isBoolean() || returnType.isNothing())
                && valueParameters.size == 1 && valueParameters[0].type == parentClass.defaultType.replaceArgumentsWithStarProjections()
                && contextReceiverParameters.isEmpty() && extensionReceiverParameter == null
    }
}