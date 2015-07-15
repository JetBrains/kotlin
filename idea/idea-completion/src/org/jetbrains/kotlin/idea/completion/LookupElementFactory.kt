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
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.KotlinLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.handlers.*
import org.jetbrains.kotlin.idea.util.TypeNullability
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils

public class LookupElementFactory(
        private val resolutionFacade: ResolutionFacade,
        private val receiverTypes: Collection<JetType>
) {
    public fun createLookupElement(
            descriptor: DeclarationDescriptor,
            boldImmediateMembers: Boolean,
            qualifyNestedClasses: Boolean = false,
            includeClassTypeArguments: Boolean = true
    ): LookupElement {
        val _descriptor = if (descriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(descriptor)
        else
            descriptor
        var element = createLookupElement(_descriptor, DescriptorToSourceUtils.descriptorToDeclaration(_descriptor), qualifyNestedClasses, includeClassTypeArguments)

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
        NORMAL,
        BOLD,
        GRAYED
    }

    public fun createLookupElementForJavaClass(psiClass: PsiClass, qualifyNestedClasses: Boolean = false, includeClassTypeArguments: Boolean = true): LookupElement {
        val lookupObject = object : DeclarationLookupObjectImpl(null, psiClass, resolutionFacade) {
            override fun getIcon(flags: Int) = psiClass.getIcon(flags)
        }
        var element = LookupElementBuilder.create(lookupObject, psiClass.getName()!!)
                .withInsertHandler(KotlinClassInsertHandler)

        val typeParams = psiClass.getTypeParameters()
        if (includeClassTypeArguments && typeParams.isNotEmpty()) {
            element = element.appendTailText(typeParams.map { it.getName() }.joinToString(", ", "<", ">"), true)
        }

        val qualifiedName = psiClass.getQualifiedName()!!
        var containerName = qualifiedName.substringBeforeLast('.', FqName.ROOT.toString())

        if (qualifyNestedClasses) {
            val nestLevel = psiClass.parents.takeWhile { it is PsiClass }.count()
            if (nestLevel > 0) {
                var itemText = psiClass.getName()
                for (i in 1..nestLevel) {
                    itemText = containerName.substringAfterLast('.') + "." + itemText
                    containerName = containerName.substringBeforeLast('.', FqName.ROOT.toString())
                }
                element = element.withPresentableText(itemText!!)
            }
        }

        element = element.appendTailText(" ($containerName)", true)

        if (lookupObject.isDeprecated) {
            element = element.setStrikeout(true)
        }

        return element.withIconFromLookupObject()
    }

    private fun createLookupElement(
            descriptor: DeclarationDescriptor,
            declaration: PsiElement?,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean
    ): LookupElement {
        if (descriptor is ClassifierDescriptor &&
            declaration is PsiClass &&
            declaration !is KotlinLightClass) {
            // for java classes we create special lookup elements
            // because they must be equal to ones created in TypesCompletion
            // otherwise we may have duplicates
            return createLookupElementForJavaClass(declaration, qualifyNestedClasses, includeClassTypeArguments)
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

        val lookupObject = object : DeclarationLookupObjectImpl(descriptor, declaration, resolutionFacade) {
            override fun getIcon(flags: Int) = JetDescriptorIconProvider.getIcon(nameAndIconDescriptor, iconDeclaration, flags)
        }
        var element = LookupElementBuilder.create(lookupObject, name)

        val insertHandler = getDefaultInsertHandler(descriptor)
        element = element.withInsertHandler(insertHandler)

        when (descriptor) {
            is FunctionDescriptor -> {
                val returnType = descriptor.getReturnType()
                element = element.withTypeText(if (returnType != null) DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) else "")

                val insertsLambda = (insertHandler as KotlinFunctionInsertHandler).lambdaInfo != null
                if (insertsLambda) {
                    element = element.appendTailText(" {...} ", false)
                }

                element = element.appendTailText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(descriptor), insertsLambda)
            }

            is VariableDescriptor -> {
                element = element.withTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType()))
            }

            is ClassDescriptor -> {
                val typeParams = descriptor.getTypeConstructor().getParameters()
                if (includeClassTypeArguments && typeParams.isNotEmpty()) {
                    element = element.appendTailText(typeParams.map { it.getName().asString() }.joinToString(", ", "<", ">"), true)
                }

                var container = descriptor.getContainingDeclaration()

                if (qualifyNestedClasses) {
                    element = element.withPresentableText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderClassifierName(descriptor))

                    while (container is ClassDescriptor) {
                        container = container.getContainingDeclaration()
                    }
                }

                if (container is PackageFragmentDescriptor || container is ClassDescriptor) {
                    element = element.appendTailText(" (" + DescriptorUtils.getFqName(container) + ")", true)
                }
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

        if (lookupObject.isDeprecated) {
            element = element.withStrikeoutness(true)
        }

        if (insertHandler is KotlinFunctionInsertHandler && insertHandler.lambdaInfo != null) {
            element.putUserData(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, Unit)
        }

        return element.withIconFromLookupObject()
    }

    private fun LookupElement.withIconFromLookupObject(): LookupElement {
        // add icon in renderElement only to pass presentation.isReal()
        return object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setIcon(DefaultLookupItemRenderer.getRawIcon(this@withIconFromLookupObject, presentation.isReal()))
            }
        }
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
                if (descriptor !is CallableMemberDescriptor) {
                    return CallableWeight.local
                }

                val container = descriptor.getContainingDeclaration()
                return when (container) {
                    is PackageFragmentDescriptor -> CallableWeight.global

                    is ClassifierDescriptor -> {
                        if (descriptor.getKind() == CallableMemberDescriptor.Kind.DECLARATION)
                            CallableWeight.thisClassMember
                        else
                            CallableWeight.baseClassMember
                    }

                    else -> CallableWeight.local
                }
            }
        }
    }

    companion object {
        public fun getDefaultInsertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement> {
            return when (descriptor) {
                is FunctionDescriptor -> {
                    val parameters = descriptor.getValueParameters()
                    when (parameters.size()) {
                        0 -> KotlinFunctionInsertHandler.NO_PARAMETERS_HANDLER

                        1 -> {
                            val parameterType = parameters.single().getType()
                            if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
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
