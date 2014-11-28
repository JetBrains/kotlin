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
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.JetDescriptorIconProvider
import org.jetbrains.jet.plugin.completion.handlers.*
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.psi.PsiClass
import org.jetbrains.jet.asJava.KotlinLightClass
import org.jetbrains.jet.lang.resolve.java.JavaResolverUtils
import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import java.awt.Color
import org.jetbrains.jet.lang.types.TypeUtils

public open class LookupElementFactory protected() {
    public open fun createLookupElement(resolutionFacade: ResolutionFacade, descriptor: DeclarationDescriptor): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        return createLookupElement(resolutionFacade, _descriptor, DescriptorToSourceUtils.descriptorToDeclaration(_descriptor))
    }

    public fun createLookupElementForJavaClass(psiClass: PsiClass): LookupElement {
        return JavaPsiClassReferenceElement(psiClass).setInsertHandler(KotlinClassInsertHandler)
    }

    private fun createLookupElement(
            analyzer: ResolutionFacade,
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?
    ): LookupElement {
        if (descriptor is ClassifierDescriptor &&
            declaration is PsiClass &&
            declaration !is KotlinLightClass &&
            !JavaResolverUtils.isCompiledKotlinClass(declaration)) {
            // for java classes we create special lookup elements
            // because they must be equal to ones created in TypesCompletion
            // otherwise we may have duplicates
            return createLookupElementForJavaClass(declaration)
        }


        var element = LookupElementBuilder.create(DeclarationDescriptorLookupObject(descriptor, analyzer, declaration), descriptor.getName().asString())
                .withIcon(JetDescriptorIconProvider.getIcon(descriptor, declaration, Iconable.ICON_FLAG_VISIBILITY))

        when (descriptor) {
            is FunctionDescriptor -> {
                val returnType = descriptor.getReturnType()
                element = element.withTypeText(if (returnType != null) DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) else "")
                element = element.appendTailText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(descriptor), false)
            }

            is VariableDescriptor -> {
                element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType()))
            }

            is ClassDescriptor -> {
                val typeParams = descriptor.getTypeConstructor().getParameters()
                if (typeParams.isNotEmpty()) {
                    element = element.appendTailText(typeParams.map { it.getName().asString() }.joinToString(", ", "<", ">"), true)
                }

                element = element.appendTailText(" (" + DescriptorUtils.getFqName(descriptor.getContainingDeclaration()) + ")", true)
            }

            else -> {
                element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor))
            }
        }

        if (descriptor is CallableDescriptor) {
            val receiver = descriptor.getExtensionReceiverParameter()
            if (receiver != null) {
                val container = descriptor.getContainingDeclaration()
                val containerPresentation = if (container is ClassDescriptor)
                    DescriptorUtils.getFqNameFromTopLevelClass(container)
                else
                    DescriptorUtils.getFqName(container)
                val receiverPresentation = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(receiver.getType())
                element = element.appendTailText(" for $receiverPresentation in $containerPresentation", true)
            }
        }

        if (KotlinBuiltIns.isDeprecated(descriptor)) {
            element = element.withStrikeoutness(true)
        }

        val insertHandler = getDefaultInsertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        if (insertHandler is KotlinFunctionInsertHandler && insertHandler.lambdaInfo != null) {
            element.putUserData<Boolean>(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, true)
        }

        return element
    }

    class object {
        public val DEFAULT: LookupElementFactory = LookupElementFactory()

        public fun getDefaultInsertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement> {
            return when (descriptor) {
                is FunctionDescriptor -> {
                    val parameters = descriptor.getValueParameters()
                    when (parameters.size) {
                        0 ->  KotlinFunctionInsertHandler.NO_PARAMETERS_HANDLER

                        1 -> {
                            val parameterType = parameters.single().getType()
                            if (KotlinBuiltIns.isFunctionOrExtensionFunctionType(parameterType)) {
                                val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
                                if (parameterCount <= 1) {
                                    // otherwise additional item with lambda template is to be added
                                    return KotlinFunctionInsertHandler(CaretPosition.IN_BRACKETS, GenerateLambdaInfo(parameterType, false))
                                }
                            }
                            KotlinFunctionInsertHandler.WITH_PARAMETERS_HANDLER
                        }

                        else -> KotlinFunctionInsertHandler.WITH_PARAMETERS_HANDLER
                    }
                }

                is PropertyDescriptor -> KotlinPropertyInsertHandler

                is ClassDescriptor -> KotlinClassInsertHandler

                else -> BaseDeclarationInsertHandler()
            }
        }
    }
}

public class BoldImmediateLookupElementFactory(private val receiverTypes: Collection<JetType>) : LookupElementFactory() {
    override fun createLookupElement(resolutionFacade: ResolutionFacade, descriptor: DeclarationDescriptor): LookupElement {
        val element = super.createLookupElement(resolutionFacade, descriptor)

        if (descriptor !is CallableMemberDescriptor) return element

        val isReceiverNullable = receiverTypes.all { it.isNullable() }
        val receiverParameter = descriptor.getExtensionReceiverParameter()

        val style: Style = if (receiverParameter != null) {
            val receiverParamType = receiverParameter.getType()
            if (isReceiverNullable && !receiverParamType.isNullable())
                Style.GRAYED
            else if (receiverTypes.any { TypeUtils.equalTypes(it, receiverParamType) })
                Style.BOLD
            else
                Style.NORMAL
        }
        else {
            if (isReceiverNullable)
                Style.GRAYED
            else if (descriptor.getContainingDeclaration() is ClassifierDescriptor && descriptor.getKind() == CallableMemberDescriptor.Kind.DECLARATION)
                Style.BOLD
            else
                Style.NORMAL
        }

        return if (style != Style.NORMAL) {
            object : LookupElementDecorator<LookupElement>(element) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    if (style == Style.BOLD) {
                        presentation.setItemTextBold(true)
                    }
                    else {
                        presentation.setItemTextForeground(Color.GRAY)
                        // gray all tail fragments too:
                        val fragments = presentation.getTailFragments()
                        presentation.clearTail()
                        for (fragment in fragments) {
                            presentation.appendTailText(fragment.text, true)
                        }
                    }
                }
            }
        }
        else {
            element
        }
    }

    private enum class Style {
        NORMAL
        BOLD
        GRAYED
    }
}
