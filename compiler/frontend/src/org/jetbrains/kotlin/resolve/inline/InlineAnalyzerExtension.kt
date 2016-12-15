/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.inline;

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.isInlineOnlyOrReified
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.FunctionAnalyzerExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue

object InlineAnalyzerExtension : FunctionAnalyzerExtension.AnalyzerExtension {

    override fun process(descriptor: FunctionDescriptor, function: KtNamedFunction, trace: BindingTrace) {
        assert(InlineUtil.isInline(descriptor)) { "This method should be invoked on inline function: " + descriptor }

        checkDefaults(descriptor, function, trace)
        checkModalityAndOverrides(descriptor, function, trace)
        checkHasInlinableAndNullability(descriptor, function, trace)

        val visitor = object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)
                element.acceptChildren(this)
            }

            override fun visitClass(klass: KtClass) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(klass, klass, descriptor))
            }

            override fun visitNamedFunction(function: KtNamedFunction) {
                if (function.parent.parent is KtObjectDeclaration) {
                    super.visitNamedFunction(function)
                }
                else {
                    trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(function, function, descriptor))
                }
            }
        }

        function.acceptChildren(visitor)
    }

    private fun checkDefaults(
            functionDescriptor: FunctionDescriptor,
            function: KtFunction,
            trace: BindingTrace) {
        val ktParameters = function.valueParameters
        for (parameter in functionDescriptor.valueParameters) {
            if (parameter.hasDefaultValue()) {
                val ktParameter = ktParameters[parameter.index]
                //report unsupported default only on inlinable lambda and on parameter with inherited default (there is some problems to inline it)
                if (checkInlinableParameter(parameter, ktParameter, functionDescriptor, null) || !parameter.declaresDefaultValue()) {
                    trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(ktParameter, ktParameter, functionDescriptor))
                }
            }
        }
    }

    private fun checkModalityAndOverrides(
            functionDescriptor: FunctionDescriptor,
            function: KtFunction,
            trace: BindingTrace) {
        if (functionDescriptor.containingDeclaration is PackageFragmentDescriptor) {
            return
        }

        if (Visibilities.isPrivate(functionDescriptor.visibility)) {
            return
        }

        val overridesAnything = functionDescriptor.overriddenDescriptors.isNotEmpty()

        if (overridesAnything) {
            val ktTypeParameters = function.typeParameters
            for (typeParameter in functionDescriptor.typeParameters) {
                if (typeParameter.isReified) {
                    val ktTypeParameter = ktTypeParameters[typeParameter.index]
                    val reportOn = ktTypeParameter.modifierList?.getModifier(KtTokens.REIFIED_KEYWORD) ?: ktTypeParameter
                    trace.report(Errors.REIFIED_TYPE_PARAMETER_IN_OVERRIDE.on(reportOn))
                }
            }
        }

        if (functionDescriptor.modality == Modality.FINAL) {
            if (overridesAnything) {
                trace.report(Errors.OVERRIDE_BY_INLINE.on(function))
            }
            return
        }

        trace.report(Errors.DECLARATION_CANT_BE_INLINED.on(function))
    }


    private fun checkHasInlinableAndNullability(
            functionDescriptor: FunctionDescriptor,
            function: KtFunction,
            trace: BindingTrace) {
        var hasInlinable = false
        val parameters = functionDescriptor.valueParameters
        var index = 0
        for (parameter in parameters) {
            hasInlinable = hasInlinable or checkInlinableParameter(parameter, function.valueParameters[index++], functionDescriptor, trace)
        }

        hasInlinable = hasInlinable or InlineUtil.containsReifiedTypeParameters(functionDescriptor)

        if (!hasInlinable && !functionDescriptor.isInlineOnlyOrReified()) {
            val modifierList = function.modifierList
            val inlineModifier = modifierList?.getModifier(KtTokens.INLINE_KEYWORD)
            val reportOn = inlineModifier ?: function
            trace.report(Errors.NOTHING_TO_INLINE.on(reportOn, functionDescriptor))
        }
    }

    fun checkInlinableParameter(
            parameter: ParameterDescriptor,
            expression: KtElement,
            functionDescriptor: CallableDescriptor,
            trace: BindingTrace?): Boolean {
        if (InlineUtil.isInlineLambdaParameter(parameter)) {
            if (parameter.type.isMarkedNullable) {
                trace?.report(Errors.NULLABLE_INLINE_PARAMETER.on(expression, expression, functionDescriptor))
            }
            else {
                return true
            }
        }
        return false
    }
}
