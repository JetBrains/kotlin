/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.JetDescriptorIconProvider
import org.jetbrains.jet.plugin.completion.handlers.CaretPosition
import org.jetbrains.jet.plugin.completion.handlers.GenerateLambdaInfo
import org.jetbrains.jet.plugin.completion.handlers.JetClassInsertHandler
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler
import org.jetbrains.jet.renderer.DescriptorRenderer

public object DescriptorLookupConverter {
    public fun createLookupElement(analyzer: KotlinCodeAnalyzer, descriptor: DeclarationDescriptor, declaration: PsiElement?): LookupElement {
        var element = LookupElementBuilder.create(DeclarationLookupObject(descriptor, analyzer, declaration), descriptor.getName().asString())

        var presentableText = descriptor.getName().asString()
        var typeText = ""
        var tailText = ""

        if (descriptor is FunctionDescriptor) {
            val returnType = descriptor.getReturnType()
            typeText = if (returnType != null) DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) else ""
            presentableText += DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(descriptor)

            if (descriptor.getReceiverParameter() != null) {
                tailText += " for " + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getReceiverParameter()!!.getType())
                tailText += " in " + DescriptorUtils.getFqName(descriptor.getContainingDeclaration())
            }
        }
        else if (descriptor is VariableDescriptor) {
            typeText = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType())
        }
        else if (descriptor is ClassDescriptor) {
            tailText = " (" + DescriptorUtils.getFqName(descriptor.getContainingDeclaration()) + ")"
        }
        else {
            typeText = DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor)
        }

        val insertHandler = getDefaultInsertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        if (insertHandler is JetFunctionInsertHandler && insertHandler.lambdaInfo != null) {
            element.putUserData<Boolean>(JetCompletionCharFilter.ACCEPT_OPENING_BRACE, true)
        }

        element = element.withTailText(tailText, true).withTypeText(typeText).withPresentableText(presentableText)
        element = element.withIcon(JetDescriptorIconProvider.getIcon(descriptor, declaration, Iconable.ICON_FLAG_VISIBILITY))
        element = element.withStrikeoutness(KotlinBuiltIns.getInstance().isDeprecated(descriptor))

        return element
    }

    public fun getDefaultInsertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement>? {
        return when (descriptor) {
            is FunctionDescriptor -> {
                val parameters = descriptor.getValueParameters()
                when (parameters.size) {
                    0 ->  JetFunctionInsertHandler.NO_PARAMETERS_HANDLER

                    1 -> {
                        val parameterType = parameters.single().getType()
                        if (KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(parameterType)) {
                            val parameterCount = KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(parameterType).size()
                            if (parameterCount <= 1) {
                                // otherwise additional item with lambda template is to be added
                                return JetFunctionInsertHandler(CaretPosition.IN_BRACKETS, GenerateLambdaInfo(parameterType, false))
                            }
                        }
                        JetFunctionInsertHandler.WITH_PARAMETERS_HANDLER
                    }

                    else -> JetFunctionInsertHandler.WITH_PARAMETERS_HANDLER
                }
            }

            is ClassDescriptor -> JetClassInsertHandler

            else -> null
        }
    }

    public fun createLookupElement(analyzer: KotlinCodeAnalyzer, descriptor: DeclarationDescriptor): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        return createLookupElement(analyzer, _descriptor, DescriptorToSourceUtils.descriptorToDeclaration(_descriptor))
    }
}
