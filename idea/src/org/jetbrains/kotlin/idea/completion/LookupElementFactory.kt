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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.completion.handlers.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.KotlinLightClass
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import com.intellij.codeInsight.lookup.DefaultLookupItemRenderer
import org.jetbrains.kotlin.types.TypeUtils
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.idea.util.TypeNullability

public class LookupElementFactory(
        private val receiverTypes: Collection<JetType>
) {
    public fun createLookupElement(
            resolutionFacade: ResolutionFacade,
            descriptor: DeclarationDescriptor,
            boldImmediateMembers: Boolean
    ): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        var element = createLookupElement(resolutionFacade, _descriptor, DescriptorToSourceUtils.descriptorToDeclaration(_descriptor))

        val weight = callableWeight(descriptor)
        if (weight != null) {
            element.putUserData(CALLABLE_WEIGHT_KEY, weight) // store for use in lookup elements sorting
        }

        if (boldImmediateMembers) {
            element = element.boldIfImmediate(weight)
        }
        return element
    }

    private fun LookupElement.boldIfImmediate(weight: CallableWeight?): LookupElement {
        val style = when (weight) {
            CallableWeight.thisClassMember, CallableWeight.thisTypeExtension -> Style.BOLD
            CallableWeight.notApplicableReceiverNullable -> Style.GRAYED
            else -> Style.NORMAL
        }
        return if (style != Style.NORMAL) {
            object : LookupElementDecorator<LookupElement>(this) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    if (style == Style.BOLD) {
                        presentation.setItemTextBold(true)
                    }
                    else {
                        presentation.setItemTextForeground(LookupCellRenderer.getGrayedForeground(false))
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
            this
        }
    }

    private enum class Style {
        NORMAL
        BOLD
        GRAYED
    }

    public fun createLookupElementForJavaClass(psiClass: PsiClass): LookupElement {
        var element = LookupElementBuilder.create(psiClass, psiClass.getName()).withInsertHandler(KotlinClassInsertHandler)

        val typeParams = psiClass.getTypeParameters()
        if (typeParams.isNotEmpty()) {
            element = element.appendTailText(typeParams.map { it.getName() }.joinToString(", ", "<", ">"), true)
        }

        val qualifiedName = psiClass.getQualifiedName()
        val dotIndex = qualifiedName.lastIndexOf('.')
        val packageName = if (dotIndex <= 0) "<root>" else qualifiedName.substring(0, dotIndex)
        element = element.appendTailText(" ($packageName)", true)

        if (psiClass.isDeprecated()) {
            element = element.setStrikeout(true)
        }

        // add icon in renderElement only to pass presentation.isReal()
        return object : LookupElementDecorator<LookupElement>(element) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setIcon(DefaultLookupItemRenderer.getRawIcon(element, presentation.isReal()))
            }
        }
    }

    private fun createLookupElement(
            resolutionFacade: ResolutionFacade,
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?
    ): LookupElement {
        if (descriptor is ClassifierDescriptor &&
            declaration is PsiClass &&
            declaration !is KotlinLightClass) {
            // for java classes we create special lookup elements
            // because they must be equal to ones created in TypesCompletion
            // otherwise we may have duplicates
            return createLookupElementForJavaClass(declaration)
        }

        // for constructor use name and icon of containing class
        val nameAndIconDescriptor: DeclarationDescriptor
        val iconDeclaration: PsiElement?
        if (descriptor is ConstructorDescriptor) {
            nameAndIconDescriptor = descriptor.getContainingDeclaration()
            iconDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(nameAndIconDescriptor)
        }
        else {
            nameAndIconDescriptor = descriptor
            iconDeclaration = declaration
        }
        val name = nameAndIconDescriptor.getName().asString()
        val icon = JetDescriptorIconProvider.getIcon(nameAndIconDescriptor, iconDeclaration, Iconable.ICON_FLAG_VISIBILITY)

        var element = LookupElementBuilder.create(DeclarationDescriptorLookupObject(descriptor, resolutionFacade, declaration), name)
                .withIcon(icon)

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
            if (descriptor.getExtensionReceiverParameter() != null) {
                val container = descriptor.getContainingDeclaration()
                val containerPresentation = if (container is ClassDescriptor) {
                    DescriptorUtils.getFqNameFromTopLevelClass(container).toString()
                }
                else {
                    DescriptorUtils.getFqName(container).toString()
                }
                val originalReceiver = descriptor.getOriginal().getExtensionReceiverParameter()!!
                val receiverPresentation = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(originalReceiver.getType())
                element = element.appendTailText(" for $receiverPresentation in $containerPresentation", true)
            }
            else {
                val container = descriptor.getContainingDeclaration()
                if (container is PackageFragmentDescriptor) { // we show container only for global functions and properties
                    //TODO: it would be probably better to show it also for static declarations which are not from the current class (imported)
                    element = element.appendTailText(" (${container.fqName})", true)
                }
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

    private fun callableWeight(descriptor: DeclarationDescriptor): CallableWeight? {
        if (descriptor !is CallableDescriptor) return null

        val isReceiverNullable = receiverTypes.isNotEmpty() && receiverTypes.all { it.nullability() == TypeNullability.NULLABLE }
        val receiverParameter = descriptor.getExtensionReceiverParameter()

        if (receiverParameter != null) {
            val receiverParamType = receiverParameter.getType()
            return if (isReceiverNullable && receiverParamType.nullability() == TypeNullability.NOT_NULL)
                CallableWeight.notApplicableReceiverNullable
            else if (receiverTypes.any { TypeUtils.equalTypes(it, receiverParamType) })
                CallableWeight.thisTypeExtension
            else
                CallableWeight.baseTypeExtension
        }
        else {
            if (isReceiverNullable) {
                return CallableWeight.notApplicableReceiverNullable
            }
            else {
                val container = descriptor.getContainingDeclaration()
                return when (container) {
                    is PackageFragmentDescriptor -> CallableWeight.global

                    is ClassifierDescriptor -> {
                        if ((descriptor as CallableMemberDescriptor).getKind() == CallableMemberDescriptor.Kind.DECLARATION)
                            CallableWeight.thisClassMember
                        else
                            CallableWeight.baseClassMember
                    }

                    else -> CallableWeight.local
                }
            }
        }
    }

    class object {
        public fun getDefaultInsertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement> {
            return when (descriptor) {
                is FunctionDescriptor -> {
                    val parameters = descriptor.getValueParameters()
                    when (parameters.size()) {
                        0 -> KotlinFunctionInsertHandler.NO_PARAMETERS_HANDLER

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
