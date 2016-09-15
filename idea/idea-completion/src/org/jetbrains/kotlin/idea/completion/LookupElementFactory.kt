/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.impl.LookupCellRenderer
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.completion.handlers.GenerateLambdaInfo
import org.jetbrains.kotlin.idea.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.lambdaPresentation
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ReceiverType
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.util.getValueParametersCountFromFunctionType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.synthetic.SamAdapterExtensionFunctionDescriptor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

interface AbstractLookupElementFactory {
    fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement>

    fun createLookupElement(
            descriptor: DeclarationDescriptor,
            useReceiverTypes: Boolean,
            qualifyNestedClasses: Boolean = false,
            includeClassTypeArguments: Boolean = true,
            parametersAndTypeGrayed: Boolean = false
    ): LookupElement?
}

data /* we need copy() */
class LookupElementFactory(
        val basicFactory: BasicLookupElementFactory,
        private val receiverTypes: Collection<ReceiverType>?,
        private val callType: CallType<*>,
        private val inDescriptor: DeclarationDescriptor,
        private val contextVariablesProvider: ContextVariablesProvider,
        private val standardLookupElementsPostProcessor: (LookupElement) -> LookupElement = { it }
) : AbstractLookupElementFactory {
    companion object {
        fun hasSingleFunctionTypeParameter(descriptor: FunctionDescriptor): Boolean {
            val parameter = descriptor.original.valueParameters.singleOrNull() ?: return false
            return parameter.type.isFunctionType
        }
    }

    val insertHandlerProvider = basicFactory.insertHandlerProvider

    private val superFunctions: Set<FunctionDescriptor> by lazy {
        inDescriptor.parentsWithSelf
                .takeWhile { it !is ClassDescriptor }
                .filterIsInstance<FunctionDescriptor>()
                .toList()
                .flatMap { it.findOriginalTopMostOverriddenDescriptors() }
                .toSet()
    }

    override fun createStandardLookupElementsForDescriptor(descriptor: DeclarationDescriptor, useReceiverTypes: Boolean): Collection<LookupElement> {
        val result = SmartList<LookupElement>()

        val isNormalCall = callType == CallType.DEFAULT || callType == CallType.DOT || callType == CallType.SAFE || callType == CallType.SUPER_MEMBERS

        result.add(createLookupElement(descriptor, useReceiverTypes, parametersAndTypeGrayed = !isNormalCall && callType != CallType.INFIX))

        // add special item for function with one argument of function type with more than one parameter
        if (descriptor is FunctionDescriptor && isNormalCall) {
            if (callType != CallType.SUPER_MEMBERS) {
                result.addSpecialFunctionCallElements(descriptor, useReceiverTypes)
            }
            else if (useReceiverTypes) {
                result.addIfNotNull(createSuperFunctionCallWithArguments(descriptor))
            }
        }

        // special "[]" item for get-operator
        if (callType == CallType.DOT && descriptor is FunctionDescriptor && descriptor.isOperator && descriptor.name == OperatorNameConventions.GET) {
            val baseLookupElement = createLookupElement(descriptor, useReceiverTypes)
            val lookupElement = object : LookupElementDecorator<LookupElement>(baseLookupElement) {
                override fun getLookupString() = "[]"
                override fun getAllLookupStrings() = setOf(lookupString)

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText = lookupString
                }

                override fun handleInsert(context: InsertionContext) {
                    val startOffset = context.startOffset
                    assert(context.document.charsSequence[startOffset - 1] == '.')
                    context.document.deleteString(startOffset - 1, startOffset)
                    context.editor.moveCaret(startOffset)

                    AutoPopupController.getInstance(context.project)?.autoPopupParameterInfo(context.editor, null)
                }
            }
            lookupElement.assignPriority(ItemPriority.GET_OPERATOR)
            result.add(lookupElement)
        }

        return result.map(standardLookupElementsPostProcessor)
    }

    private fun MutableCollection<LookupElement>.addSpecialFunctionCallElements(descriptor: FunctionDescriptor, useReceiverTypes: Boolean) {
        // check that all parameters except for the last one are optional
        val lastParameter = descriptor.valueParameters.lastOrNull() ?: return
        if (!descriptor.valueParameters.all { it == lastParameter || it.hasDefaultValue() }) return

        if (lastParameter.original.type.isFunctionType) {
            val isSingleParameter = descriptor.valueParameters.size == 1

            val parameterType = lastParameter.type
            val insertHandler = insertHandlerProvider.insertHandler(descriptor) as KotlinFunctionInsertHandler.Normal
            if (insertHandler.lambdaInfo == null) {
                val functionParameterCount = getValueParametersCountFromFunctionType(parameterType)
                add(createFunctionCallElementWithLambda(descriptor, parameterType, functionParameterCount > 1, useReceiverTypes))
            }

            if (isSingleParameter) {
                //TODO: also ::function? at least for local functions
                //TODO: order for them
                val fuzzyParameterType = parameterType.toFuzzyType(descriptor.typeParameters)
                for ((variable, substitutor) in contextVariablesProvider.functionTypeVariables(fuzzyParameterType)) {
                    val substitutedDescriptor = descriptor.substitute(substitutor)
                    add(createFunctionCallElementWithArguments(substitutedDescriptor, variable.name.render(), useReceiverTypes))
                }
            }
        }
    }

    private fun createFunctionCallElementWithLambda(descriptor: FunctionDescriptor, parameterType: KotlinType, explicitLambdaParameters: Boolean, useReceiverTypes: Boolean): LookupElement {
        var lookupElement = createLookupElement(descriptor, useReceiverTypes)
        val inputTypeArguments = (insertHandlerProvider.insertHandler(descriptor) as KotlinFunctionInsertHandler.Normal).inputTypeArguments
        val lambdaInfo = GenerateLambdaInfo(parameterType, explicitLambdaParameters)
        val lambdaPresentation = lambdaPresentation(if (explicitLambdaParameters) parameterType else null)

        // render only the last parameter because all other should be optional and will be omitted
        var parametersRenderer = BasicLookupElementFactory.SHORT_NAMES_RENDERER
        if (descriptor.valueParameters.size > 1) {
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
                KotlinFunctionInsertHandler.Normal(callType, inputTypeArguments, inputValueArguments = false, lambdaInfo = lambdaInfo).handleInsert(context, this)
            }
        }

        return lookupElement
    }

    private fun createSuperFunctionCallWithArguments(descriptor: FunctionDescriptor): LookupElement? {
        if (descriptor.valueParameters.isEmpty()) return null
        if (descriptor.findOriginalTopMostOverriddenDescriptors().none { it in superFunctions }) return null

        val argumentText = descriptor.valueParameters.map {
            (if (it.varargElementType != null) "*" else "") + it.name.render()
        }.joinToString(", ") //TODO: use code formatting settings

        val lookupElement = createFunctionCallElementWithArguments(descriptor, argumentText, true)
        lookupElement.assignPriority(ItemPriority.SUPER_METHOD_WITH_ARGUMENTS)
        lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
        return lookupElement
    }

    private fun createFunctionCallElementWithArguments(descriptor: FunctionDescriptor, argumentText: String, useReceiverTypes: Boolean): LookupElement {
        val lookupElement = createLookupElement(descriptor, useReceiverTypes)

        val needTypeArguments = (insertHandlerProvider.insertHandler(descriptor) as KotlinFunctionInsertHandler.Normal).inputTypeArguments
        return FunctionCallWithArgumentsLookupElement(lookupElement, descriptor, argumentText, needTypeArguments)
    }

    private inner class FunctionCallWithArgumentsLookupElement(
            originalLookupElement: LookupElement,
            private val descriptor: FunctionDescriptor,
            private val argumentText: String,
            private val needTypeArguments: Boolean
    ) : LookupElementDecorator<LookupElement>(originalLookupElement) {

        override fun equals(other: Any?) = other is FunctionCallWithArgumentsLookupElement && delegate == other.delegate && argumentText == other.argumentText
        override fun hashCode() = delegate.hashCode() * 17 + argumentText.hashCode()

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)

            presentation.clearTail()
            presentation.appendTailText("($argumentText)", false)
            basicFactory.appendContainerAndReceiverInformation(descriptor) { presentation.appendTailText(it, true) }
        }

        override fun handleInsert(context: InsertionContext) {
            KotlinFunctionInsertHandler.Normal(callType, inputTypeArguments = needTypeArguments, inputValueArguments = false, argumentText = argumentText)
                    .handleInsert(context, this)
        }
    }

    override fun createLookupElement(
            descriptor: DeclarationDescriptor,
            useReceiverTypes: Boolean,
            qualifyNestedClasses: Boolean,
            includeClassTypeArguments: Boolean,
            parametersAndTypeGrayed: Boolean
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
        val style = when (weight?.enum) {
            CallableWeightEnum.thisClassMember, CallableWeightEnum.thisTypeExtension -> Style.BOLD
            CallableWeightEnum.receiverCastRequired -> Style.GRAYED
            else -> Style.NORMAL
        }
        return if (style != Style.NORMAL) {
            object : LookupElementDecorator<LookupElement>(this) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    if (style == Style.BOLD) {
                        presentation.isItemTextBold = true
                    }
                    else {
                        presentation.itemTextForeground = LookupCellRenderer.getGrayedForeground(false)
                        // gray all tail fragments too:
                        val fragments = presentation.tailFragments
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

        // don't treat synthetic extensions as real extensions
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return callableWeight(descriptor.getMethod)
        }
        if (descriptor is SamAdapterExtensionFunctionDescriptor) {
            return callableWeight(descriptor.baseDescriptorForSynthetic)
        }

        if (descriptor.overriddenDescriptors.isNotEmpty()) {
            // Optimization: when one of direct overridden fits, then nothing can fit better
            descriptor.overriddenDescriptors
                    .mapNotNull { it.callableWeightBasedOnReceiver(receiverTypes, onReceiverTypeMismatch = null) }
                    .minBy { it.enum }
                    ?.let { return it }

            val overridden = descriptor.overriddenTreeUniqueAsSequence(useOriginal = false)
            return overridden
                    .map { callableWeightBasic(it, receiverTypes)!! }
                    .minBy { it.enum }!!
        }

        return callableWeightBasic(descriptor, receiverTypes)
    }

    private fun callableWeightBasic(descriptor: CallableDescriptor, receiverTypes: Collection<ReceiverType>): CallableWeight? {
        descriptor.callableWeightBasedOnReceiver(receiverTypes, CallableWeight.receiverCastRequired)?.let { return it }

        return when (descriptor.containingDeclaration) {
            is PackageFragmentDescriptor, is ClassifierDescriptor -> CallableWeight.globalOrStatic
            else -> CallableWeight.local
        }
    }

    private fun CallableDescriptor.callableWeightBasedOnReceiver(
            receiverTypes: Collection<ReceiverType>,
            onReceiverTypeMismatch: CallableWeight?
    ): CallableWeight? {
        val receiverParameter = extensionReceiverParameter
                                ?: dispatchReceiverParameter
                                ?: return null

        val matchingReceiverIndices = HashSet<Int>()
        var bestReceiverType: ReceiverType? = null
        var bestWeight: CallableWeightEnum? = null
        for (receiverType in receiverTypes) {
            val weight = callableWeightForReceiverType(receiverType.type, receiverParameter.type)
            if (weight != null) {
                if (bestWeight == null || weight < bestWeight) {
                    bestWeight = weight
                    bestReceiverType = receiverType
                }
                matchingReceiverIndices.add(receiverType.receiverIndex)
            }
        }

        if (bestWeight == null) return onReceiverTypeMismatch

        val receiverIndex = bestReceiverType!!.receiverIndex

        var receiverIndexToUse: Int? = receiverIndex
        val maxReceiverIndex = receiverTypes.map { it.receiverIndex }.max()!!
        if (maxReceiverIndex > 0) {
            val matchesAllReceivers = (0..maxReceiverIndex).all { it in matchingReceiverIndices }
            if (matchesAllReceivers) { // if descriptor is matching all receivers then use null as receiverIndex - otherwise e.g. all members of Any would have too high priority
                receiverIndexToUse = null
            }
        }

        return CallableWeight(bestWeight, receiverIndexToUse)
    }

    private fun CallableDescriptor.callableWeightForReceiverType(receiverType: KotlinType, receiverParameterType: KotlinType): CallableWeightEnum? {
        if (TypeUtils.equalTypes(receiverType, receiverParameterType)) {
            return when {
                isExtensionForTypeParameter() -> CallableWeightEnum.typeParameterExtension
                isExtension -> CallableWeightEnum.thisTypeExtension
                else -> CallableWeightEnum.thisClassMember
            }
        }
        else if (receiverType.isSubtypeOf(receiverParameterType)) {
            return if (isExtension) CallableWeightEnum.baseTypeExtension else CallableWeightEnum.baseClassMember
        }
        else {
            return null
        }
    }

    private fun CallableDescriptor.isExtensionForTypeParameter(): Boolean {
        val receiverParameter = original.extensionReceiverParameter ?: return false
        val typeParameter = receiverParameter.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
        return typeParameter.containingDeclaration == original
    }
}
