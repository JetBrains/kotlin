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

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.psi.PsiClass
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.completion.handlers.GenerateLambdaInfo
import org.jetbrains.kotlin.idea.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.lambdaPresentation
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KtType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class LookupElementFactory(
        private val resolutionFacade: ResolutionFacade,
        private val receiverTypes: Collection<KtType>?,
        private val callType: CallType<*>?,
        private val isInStringTemplateAfterDollar: Boolean,
        public val insertHandlerProvider: InsertHandlerProvider,
        contextVariablesProvider: () -> Collection<VariableDescriptor>
) {
    private val basicFactory = BasicLookupElementFactory(resolutionFacade.project, insertHandlerProvider)

    private val functionTypeContextVariables by lazy(LazyThreadSafetyMode.NONE) {
        contextVariablesProvider().filter { KotlinBuiltIns.isFunctionOrExtensionFunctionType(it.type) }
    }

    public fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement> {
        val result = SmartList<LookupElement>()

        val isNormalCall = callType == CallType.DEFAULT || callType == CallType.DOT || callType == CallType.SAFE

        var lookupElement = createLookupElement(descriptor, useReceiverTypes, parametersAndTypeGrayed = !isNormalCall && callType != CallType.INFIX)
        if (isInStringTemplateAfterDollar && (descriptor is FunctionDescriptor || descriptor is ClassifierDescriptor)) {
            lookupElement = lookupElement.withBracesSurrounding()
        }
        result.add(lookupElement)

        // add special item for function with one argument of function type with more than one parameter
        if (descriptor is FunctionDescriptor && isNormalCall) {
            result.addSpecialFunctionCallElements(descriptor, useReceiverTypes)
        }

        return result
    }

    private fun MutableCollection<LookupElement>.addSpecialFunctionCallElements(descriptor: FunctionDescriptor, useReceiverTypes: Boolean) {
        // check that all parameters except for the last one are optional
        val lastParameter = descriptor.valueParameters.lastOrNull() ?: return
        if (!descriptor.valueParameters.all { it == lastParameter || it.hasDefaultValue() }) return
        val parameterType = lastParameter.type
        if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
            val isSingleParameter = descriptor.valueParameters.size() == 1

            val functionParameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
            // we don't need special item inserting lambda for single functional parameter that does not need multiple arguments because the default item will be special in this case
            if (!isSingleParameter || functionParameterCount > 1) {
                add(createFunctionCallElementWithLambda(descriptor, parameterType, functionParameterCount > 1, useReceiverTypes))
            }

            if (isSingleParameter) {
                //TODO: also ::function? at least for local functions
                //TODO: order for them
                val fuzzyParameterType = FuzzyType(parameterType, descriptor.typeParameters)
                for (variable in functionTypeContextVariables) {
                    val substitutor = variable.fuzzyReturnType()?.checkIsSubtypeOf(fuzzyParameterType)
                    if (substitutor != null) {
                        val substitutedDescriptor = descriptor.substitute(substitutor) ?: continue
                        add(createFunctionCallElementWithArgument(substitutedDescriptor, variable.name.asString(), useReceiverTypes))
                    }
                }
            }
        }
    }

    private fun createFunctionCallElementWithLambda(descriptor: FunctionDescriptor, parameterType: KtType, explicitLambdaParameters: Boolean, useReceiverTypes: Boolean): LookupElement {
        var lookupElement = createLookupElement(descriptor, useReceiverTypes)
        val inputTypeArguments = (insertHandlerProvider.insertHandler(descriptor) as KotlinFunctionInsertHandler.Normal).inputTypeArguments
        val lambdaInfo = GenerateLambdaInfo(parameterType, explicitLambdaParameters)
        val lambdaPresentation = lambdaPresentation(if (explicitLambdaParameters) parameterType else null)

        // render only the last parameter because all other should be optional and will be omitted
        var parametersRenderer = DescriptorRenderer.SHORT_NAMES_IN_TYPES
        if (descriptor.valueParameters.size() > 1) {
            parametersRenderer = parametersRenderer.withOptions {
                valueParametersHandler = object: DescriptorRenderer.ValueParametersHandler by this.valueParametersHandler {
                    override fun appendBeforeValueParameter(parameter: ValueParameterDescriptor, parameterIndex: Int, parameterCount: Int, builder: StringBuilder) {
                        builder.append("..., ")
                    }
                }
            }
        }
        val parametersPresentation = parametersRenderer.renderValueParameters(listOf(descriptor.valueParameters.last()), descriptor.hasSynthesizedParameterNames())

        lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)

                presentation.clearTail()
                presentation.appendTailText(" $lambdaPresentation ", false)
                presentation.appendTailText(parametersPresentation, true)
                basicFactory.appendContainerAndReceiverInformation(descriptor) { presentation.appendTailText(it, true) }
            }

            override fun handleInsert(context: InsertionContext) {
                KotlinFunctionInsertHandler.Normal(inputTypeArguments, inputValueArguments = false, lambdaInfo = lambdaInfo).handleInsert(context, this)
            }
        }

        if (isInStringTemplateAfterDollar) {
            lookupElement = lookupElement.withBracesSurrounding()
        }

        return lookupElement
    }

    private fun createFunctionCallElementWithArgument(descriptor: FunctionDescriptor, argumentText: String, useReceiverTypes: Boolean): LookupElement {
        var lookupElement = createLookupElement(descriptor, useReceiverTypes)

        val needTypeArguments = (insertHandlerProvider.insertHandler(descriptor) as KotlinFunctionInsertHandler.Normal).inputTypeArguments
        lookupElement = FunctionCallWithArgumentLookupElement(lookupElement, descriptor, argumentText, needTypeArguments)

        if (isInStringTemplateAfterDollar) {
            lookupElement = lookupElement.withBracesSurrounding()
        }

        return lookupElement
    }

    private inner class FunctionCallWithArgumentLookupElement(
            originalLookupElement: LookupElement,
            private val descriptor: FunctionDescriptor,
            private val argumentText: String,
            private val needTypeArguments: Boolean
    ) : LookupElementDecorator<LookupElement>(originalLookupElement) {

        override fun equals(other: Any?) = other is FunctionCallWithArgumentLookupElement && delegate == other.delegate && argumentText == other.argumentText
        override fun hashCode() = delegate.hashCode() * 17 + argumentText.hashCode()

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)

            presentation.clearTail()
            presentation.appendTailText("($argumentText)", false)
            basicFactory.appendContainerAndReceiverInformation(descriptor) { presentation.appendTailText(it, true) }
        }

        override fun handleInsert(context: InsertionContext) {
            KotlinFunctionInsertHandler.Normal(inputTypeArguments = needTypeArguments, inputValueArguments = false, argumentText = argumentText).handleInsert(context, this)
        }
    }

    public fun createLookupElement(
            descriptor: DeclarationDescriptor,
            useReceiverTypes: Boolean,
            qualifyNestedClasses: Boolean = false,
            includeClassTypeArguments: Boolean = true,
            parametersAndTypeGrayed: Boolean = false
    ): LookupElement {
        var element = basicFactory.createLookupElement(descriptor, qualifyNestedClasses, includeClassTypeArguments, parametersAndTypeGrayed)

        if (useReceiverTypes) {
            val weight = callableWeight(descriptor)
            if (weight != null) {
                element.putUserData(CALLABLE_WEIGHT_KEY, weight) // store for use in lookup elements sorting
            }

            element = element.boldIfImmediate(weight)
        }
        return element
    }

    private fun LookupElement.boldIfImmediate(weight: CallableWeight?): LookupElement {
        val style = when (weight) {
            CallableWeight.thisClassMember, CallableWeight.thisTypeExtension -> Style.BOLD
            CallableWeight.receiverCastRequired -> Style.GRAYED
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

    private fun callableWeight(descriptor: DeclarationDescriptor): CallableWeight? {
        if (receiverTypes == null) return null
        if (descriptor !is CallableDescriptor) return null

        val overridden = descriptor.overriddenDescriptors
        if (overridden.isNotEmpty()) {
            return overridden.map { callableWeight(it)!! }.min()!!
        }

        // don't treat synthetic extensions as real extensions
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return callableWeight(descriptor.getMethod)
        }
        if (descriptor is SamAdapterExtensionFunctionDescriptor) {
            return callableWeight(descriptor.sourceFunction)
        }

        val receiverParameter = descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter
        if (receiverParameter != null) {
            return if (receiverTypes.any { TypeUtils.equalTypes(it, receiverParameter.type) }) {
                when {
                    descriptor.isExtensionForTypeParameter() -> CallableWeight.typeParameterExtension
                    descriptor.isExtension -> CallableWeight.thisTypeExtension
                    else -> CallableWeight.thisClassMember
                }
            }
            else if (receiverTypes.any { it.isSubtypeOf(receiverParameter.type) }) {
                if (descriptor.isExtension) CallableWeight.baseTypeExtension else CallableWeight.baseClassMember
            }
            else {
                CallableWeight.receiverCastRequired
            }
        }

        return when (descriptor.containingDeclaration) {
            is PackageFragmentDescriptor, is ClassifierDescriptor -> CallableWeight.globalOrStatic
            else -> CallableWeight.local
        }
    }

    private fun CallableDescriptor.isExtensionForTypeParameter(): Boolean {
        val receiverParameter = original.extensionReceiverParameter ?: return false
        val typeParameter = receiverParameter.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
        return typeParameter.containingDeclaration == original
    }

    public fun createLookupElementForJavaClass(psiClass: PsiClass, qualifyNestedClasses: Boolean = false, includeClassTypeArguments: Boolean = true): LookupElement {
        return basicFactory.createLookupElementForJavaClass(psiClass, qualifyNestedClasses, includeClassTypeArguments)
    }

    public fun createLookupElementForPackage(name: FqName): LookupElement {
        return basicFactory.createLookupElementForPackage(name)
    }
}
