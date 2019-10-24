/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.getCompileTimeConstant
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberOrNullableType

object DefaultCheckerInTailrec : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (declaration !is KtNamedFunction || descriptor !is FunctionDescriptor || !descriptor.isTailrec) return

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ProperComputationOrderOfTailrecDefaultParameters)) return

        val defaultValues = descriptor.valueParameters.filter { it.declaresDefaultValue() }.filter {
            val parameterDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(it)
            if (parameterDeclaration is KtParameter) {
                parameterDeclaration.defaultValue?.let {
                    getCompileTimeConstant(
                        it,
                        context.trace.bindingContext,
                        false,
                        context.languageVersionSettings.supportsFeature(LanguageFeature.InlineConstVals)
                    )?.let { const ->
                        val type = const.getType(descriptor.module)
                        return@filter !(KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(type) ||
                                KotlinBuiltIns.isStringOrNullableString(type))
                    }
                }
            }

            true
        }

        if (defaultValues.size > 1) {
            context.trace.report(ErrorsJvm.TAILREC_WITH_DEFAULTS.on(declaration))
        }
    }
}