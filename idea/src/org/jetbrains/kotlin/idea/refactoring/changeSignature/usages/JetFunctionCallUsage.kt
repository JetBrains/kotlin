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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.ContainerUtil
import gnu.trove.TIntArrayList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.isInsideOfCallerBody
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.createNameCounterpartMap
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.sure
import java.util.*

class JetFunctionCallUsage(
        element: KtCallElement,
        private val callee: JetCallableDefinitionUsage<*>
) : JetUsageInfo<KtCallElement>(element) {
    private val context = element.analyze(BodyResolveMode.FULL)
    private val resolvedCall = element.getResolvedCall(context)

    override fun processUsage(changeInfo: JetChangeInfo, element: KtCallElement, allUsages: Array<out UsageInfo>): Boolean {
        if (shouldSkipUsage(element)) return true

        changeNameIfNeeded(changeInfo, element)

        if (element.valueArgumentList != null) {
            if (changeInfo.isParameterSetOrOrderChanged) {
                updateArgumentsAndReceiver(changeInfo, element, allUsages)
            }
            else {
                changeArgumentNames(changeInfo, element)
            }
        }

        if (changeInfo.getNewParametersCount() == 0 && element is KtDelegatorToSuperCall) {
            val enumEntry = element.getStrictParentOfType<KtEnumEntry>()
            if (enumEntry != null && enumEntry.initializerList == element.parent) {
                val initializerList = enumEntry.initializerList
                enumEntry.deleteChildRange(enumEntry.getColon() ?: initializerList, initializerList)
            }
        }

        return true
    }

    private fun shouldSkipUsage(element: KtCallElement): Boolean {
        // TODO: We probable need more clever processing of invalid calls, but for now default to Java-like behaviour
        if (resolvedCall == null && element !is KtDelegatorToSuperCall) return true
        if (resolvedCall == null || resolvedCall.isReallySuccess()) return false

        // TODO: investigate why arguments are not recorded for enum constructor call
        if (element is KtDelegatorToSuperCall && element.parent.parent is KtEnumEntry) return false

        if (!resolvedCall.call.valueArguments.all{ resolvedCall.getArgumentMapping(it) is ArgumentMatch }) return true

        val arguments = resolvedCall.valueArguments
        return !resolvedCall.resultingDescriptor.valueParameters.all{ arguments.containsKey(it) }
    }

    private val isPropertyJavaUsage: Boolean
        get() {
            val calleeElement = this.callee.element
            if (calleeElement !is KtProperty && calleeElement !is KtParameter) return false
            return resolvedCall?.resultingDescriptor is JavaMethodDescriptor
        }

    protected fun changeNameIfNeeded(changeInfo: JetChangeInfo, element: KtCallElement) {
        if (!changeInfo.isNameChanged) return

        val callee = element.calleeExpression
        if (callee !is KtSimpleNameExpression) return

        var newName = changeInfo.newName
        if (isPropertyJavaUsage) {
            val currentName = callee.getReferencedName()
            if (JvmAbi.isGetterName(currentName))
                newName = JvmAbi.getterName(newName)
            else if (JvmAbi.isSetterName(currentName)) newName = JvmAbi.setterName(newName)
        }

        callee.replace(KtPsiFactory(project).createSimpleName(newName))
    }

    private fun getReceiverExpressionIfMatched(
            receiverValue: ReceiverValue,
            originalDescriptor: DeclarationDescriptor,
            psiFactory: KtPsiFactory
    ): KtExpression? {
        if (!receiverValue.exists()) return null

        // Replace descriptor of extension function/property with descriptor of its receiver
        // to simplify checking against receiver value in the corresponding resolved call
        val adjustedDescriptor = if (originalDescriptor is CallableDescriptor && originalDescriptor !is ReceiverParameterDescriptor) {
            originalDescriptor.extensionReceiverParameter ?: return null
        }
        else originalDescriptor

        val currentIsExtension = resolvedCall!!.extensionReceiver == receiverValue
        val originalIsExtension = adjustedDescriptor is ReceiverParameterDescriptor && adjustedDescriptor.value is ExtensionReceiver
        if (currentIsExtension != originalIsExtension) return null

        val originalType = when (adjustedDescriptor) {
            is ReceiverParameterDescriptor -> adjustedDescriptor.type
            is ClassDescriptor -> adjustedDescriptor.defaultType
            else -> null
        }
        if (originalType == null || !KotlinTypeChecker.DEFAULT.isSubtypeOf(receiverValue.type, originalType)) return null

        return getReceiverExpression(receiverValue, psiFactory)
    }

    private fun needSeparateVariable(element: PsiElement): Boolean {
        return when {
            element is KtConstantExpression, element is KtThisExpression, element is KtSimpleNameExpression -> false
            element is KtBinaryExpression && OperatorConventions.ASSIGNMENT_OPERATIONS.containsKeyRaw(element.operationToken) -> true
            element is KtUnaryExpression && OperatorConventions.INCREMENT_OPERATIONS.containsRaw(element.operationToken) -> true
            element is KtCallExpression -> element.getResolvedCall(context)?.resultingDescriptor is ConstructorDescriptor
            else -> element.children.any { needSeparateVariable(it) }
        }
    }

    private fun substituteReferences(
            expression: KtExpression,
            referenceMap: Map<PsiReference, DeclarationDescriptor>,
            psiFactory: KtPsiFactory
    ): KtExpression {
        if (referenceMap.isEmpty() || resolvedCall == null) return expression

        val newExpression = expression.copy() as KtExpression

        val nameCounterpartMap = createNameCounterpartMap(expression, newExpression)

        val valueArguments = resolvedCall.valueArguments

        val replacements = ArrayList<Pair<KtElement, KtElement>>()
        loop@ for ((ref, descriptor) in referenceMap.entries) {
            var argumentExpression: KtExpression?
            val addReceiver: Boolean
            if (descriptor is ValueParameterDescriptor) {
                // Ordinary parameter
                // Find corresponding parameter in the current function (may differ from 'descriptor' if original function is part of override hierarchy)
                val parameterDescriptor = resolvedCall.resultingDescriptor.valueParameters[descriptor.index]
                val resolvedValueArgument = valueArguments[parameterDescriptor] as? ExpressionValueArgument ?: continue
                val argument = resolvedValueArgument.valueArgument ?: continue

                addReceiver = false
                argumentExpression = argument.getArgumentExpression()
            }
            else {
                addReceiver = descriptor !is ReceiverParameterDescriptor
                argumentExpression = getReceiverExpressionIfMatched(resolvedCall.extensionReceiver, descriptor, psiFactory)
                                     ?: getReceiverExpressionIfMatched(resolvedCall.dispatchReceiver, descriptor, psiFactory)
            }
            if (argumentExpression == null) continue

            if (needSeparateVariable(argumentExpression)
                && PsiTreeUtil.getNonStrictParentOfType(element,
                                                        KtConstructorDelegationCall::class.java,
                                                        KtDelegationSpecifier::class.java,
                                                        KtParameter::class.java) == null) {

                KotlinIntroduceVariableHandler.doRefactoring(project, null, argumentExpression, listOf(argumentExpression)) {
                    argumentExpression = psiFactory.createExpression(it.name!!)
                }
            }

            var expressionToReplace: KtExpression = nameCounterpartMap.getRaw(ref.element) ?: continue
            val parent = expressionToReplace.parent

            if (parent is KtThisExpression) {
                expressionToReplace = parent
            }

            if (addReceiver) {
                val callExpression = expressionToReplace.getParentOfTypeAndBranch<KtCallExpression>(true) { calleeExpression }
                when {
                    callExpression != null -> expressionToReplace = callExpression
                    parent is KtOperationExpression && parent.operationReference == expressionToReplace -> continue@loop
                }

                val replacement = psiFactory.createExpression("${argumentExpression!!.text}.${expressionToReplace.text}")
                replacements.add(expressionToReplace to replacement)
            }
            else {
                replacements.add(expressionToReplace to argumentExpression!!)
            }
        }

        // Sort by descending offset so that call arguments are replaced before call itself
        ContainerUtil.sort(replacements, REVERSED_TEXT_OFFSET_COMPARATOR)
        for ((expressionToReplace, replacingExpression) in replacements) {
            expressionToReplace.replace(replacingExpression)
        }

        return newExpression
    }

    private fun updateArgumentsAndReceiver(changeInfo: JetChangeInfo, element: KtCallElement, allUsages: Array<out UsageInfo>) {
        var arguments = element.valueArgumentList.sure { "Argument list is expected: " + element.text }
        val oldArguments = element.valueArguments

        if (isPropertyJavaUsage) return updateJavaPropertyCall(changeInfo, element)

        val isNamedCall = oldArguments.size > 1 && oldArguments[0].isNamed()

        val indicesOfArgumentsWithDefaultValues = TIntArrayList()

        val psiFactory = KtPsiFactory(element.project)

        val newSignatureParameters = changeInfo.getNonReceiverParameters()
        val newArgumentListText = newSignatureParameters
                .map { parameterInfo ->
                    val defaultValueForCall = parameterInfo.defaultValueForCall
                    val defaultValueText = when {
                        element.isInsideOfCallerBody(allUsages) ->
                            parameterInfo.name
                        defaultValueForCall != null ->
                            substituteReferences(defaultValueForCall, parameterInfo.defaultValueParameterReferences, psiFactory).text
                        else ->
                            ""
                    }
                    val argumentValue = if (defaultValueText.isEmpty()) "0" else defaultValueText
                    if (isNamedCall) "${parameterInfo.getInheritedName(callee)}=$argumentValue" else argumentValue
                }
                .joinToString(prefix = "(", postfix = ")")
        val newArgumentList = KtPsiFactory(project).createCallArguments(newArgumentListText)

        val argumentMap = getParamIndexToArgumentMap(changeInfo, oldArguments)

        val newReceiverInfo = changeInfo.receiverParameterInfo
        val originalReceiverInfo = changeInfo.methodDescriptor.receiver

        val extensionReceiver = if (resolvedCall != null) resolvedCall.extensionReceiver else ReceiverValue.NO_RECEIVER
        val dispatchReceiver = if (resolvedCall != null) resolvedCall.dispatchReceiver else ReceiverValue.NO_RECEIVER

        var elementToReplace: PsiElement = element
        val parent = element.parent
        if (parent is KtQualifiedExpression && parent.selectorExpression == element) {
            elementToReplace = parent
        }

        // Do not add extension receiver to calls with explicit dispatch receiver
        if (newReceiverInfo != null && elementToReplace is KtQualifiedExpression && dispatchReceiver is ExpressionReceiver) return

        val newArguments = newArgumentList.arguments
        var actualIndex = 0
        for (i in newArguments.indices) {
            val newArgument = newArguments[i]
            val parameterInfo = newSignatureParameters[i]
            if (parameterInfo == originalReceiverInfo) {
                val receiverExpression = getReceiverExpression(extensionReceiver, psiFactory)
                if (receiverExpression != null) {
                    newArgument.replace(receiverExpression)
                }
                actualIndex++
                continue
            }

            val oldArgument = argumentMap[parameterInfo.oldIndex]

            if (oldArgument != null) {
                val argumentName = oldArgument.getArgumentName()
                val argumentNameExpression = argumentName?.referenceExpression
                changeArgumentName(argumentNameExpression, parameterInfo)
                //noinspection ConstantConditions
                val argumentReplacement = newArgument.replace(
                        if (oldArgument is KtFunctionLiteralArgument)
                            psiFactory.createArgument(oldArgument.getArgumentExpression(), null, false)
                        else
                            oldArgument.asElement()) as ValueArgument
                argumentMap.put(parameterInfo.oldIndex, argumentReplacement)
            }
            else if (parameterInfo.defaultValueForCall == null) {
                if (parameterInfo.defaultValueForParameter != null) {
                    newArgumentList.removeArgument(newArgument)
                }
                else {
                    newArgument.delete() // keep space between commas
                }
            }
            else {
                indicesOfArgumentsWithDefaultValues.add(actualIndex++)
            }// TODO: process default arguments in the middle
        }

        val lambdaArguments = element.functionLiteralArguments
        val hasLambdaArgumentsBefore = !lambdaArguments.isEmpty()
        if (hasLambdaArgumentsBefore) {
            element.deleteChildRange(lambdaArguments.first(), lambdaArguments.last())
        }

        val lastArgument = newArgumentList.arguments.lastOrNull()
        val lastLambdaExpr = if (lastArgument != null) lastArgument.getArgumentExpression()?.unpackFunctionLiteral() else null
        val lastNewParam = changeInfo.newParameters.lastOrNull()
        val hasTrailingLambdaInArgumentListAfter =
                lastLambdaExpr != null && lastNewParam != null && argumentMap[lastNewParam.oldIndex] == lastArgument
        val newLambdaWithDefaultValueWasAdded =
                lastNewParam != null
                && lastNewParam.isNewParameter
                && lastNewParam.defaultValueForCall is KtFunctionLiteralExpression
                && lastArgument != null
                && !lastArgument.isNamed()
        val shouldMoveLambdaOut = hasTrailingLambdaInArgumentListAfter && hasLambdaArgumentsBefore || newLambdaWithDefaultValueWasAdded

        arguments = arguments.replace(newArgumentList) as KtValueArgumentList

        val argumentsToShorten = ArrayList<KtElement>(indicesOfArgumentsWithDefaultValues.size())
        val argumentList = arguments.arguments
        indicesOfArgumentsWithDefaultValues.forEach {
            argumentsToShorten.add(argumentList[it])
            true
        }
        argumentsToShorten.forEach { it.addToShorteningWaitSet(SHORTEN_ARGUMENTS_OPTIONS) }

        var newElement: KtElement = element
        if (newReceiverInfo != originalReceiverInfo) {
            val replacingElement: PsiElement
            if (newReceiverInfo != null) {
                val receiverArgument = argumentMap[newReceiverInfo.oldIndex]
                val extensionReceiverExpression = receiverArgument?.getArgumentExpression()
                val defaultValueForCall = newReceiverInfo.defaultValueForCall
                val receiver = extensionReceiverExpression?.let { psiFactory.createExpression(it.text) }
                               ?: defaultValueForCall
                               ?: psiFactory.createExpression("_")

                replacingElement = psiFactory.createExpressionByPattern("$0.$1", receiver, element)
            }
            else {
                replacingElement = psiFactory.createExpression(element.text)
            }

            newElement = elementToReplace.replace(replacingElement) as KtElement
        }

        if (shouldMoveLambdaOut) {
            val newCallExpression = ((newElement as? KtQualifiedExpression)?.selectorExpression ?: newElement) as KtCallExpression
            newCallExpression.moveFunctionLiteralOutsideParentheses()
        }
    }

    private fun changeArgumentNames(changeInfo: JetChangeInfo, element: KtCallElement) {
        for (argument in element.valueArguments) {
            val argumentName = argument.getArgumentName()
            val argumentNameExpression = argumentName?.referenceExpression ?: continue
            val oldParameterIndex = changeInfo.getOldParameterIndex(argumentNameExpression.getReferencedName()) ?: continue
            val newParameterIndex = if (changeInfo.receiverParameterInfo != null) oldParameterIndex + 1 else oldParameterIndex
            val parameterInfo = changeInfo.newParameters[newParameterIndex]
            changeArgumentName(argumentNameExpression, parameterInfo)
        }
    }

    private fun changeArgumentName(argumentNameExpression: KtSimpleNameExpression?, parameterInfo: JetParameterInfo) {
        val identifier = argumentNameExpression?.getIdentifier() ?: return
        val newName = parameterInfo.getInheritedName(callee)
        identifier.replace(KtPsiFactory(project).createIdentifier(newName))
    }

    companion object {
        private val REVERSED_TEXT_OFFSET_COMPARATOR = object : Comparator<Pair<KtElement, KtElement>> {
            override fun compare(p1: Pair<KtElement, KtElement>, p2: Pair<KtElement, KtElement>): Int {
                val offset1 = p1.first.startOffset
                val offset2 = p2.first.startOffset
                return when {
                    offset1 < offset2 -> 1
                    offset1 > offset2 -> -1
                    else -> 0
                }
            }
        }

        private val SHORTEN_ARGUMENTS_OPTIONS = ShortenReferences.Options(true, true)

        private fun updateJavaPropertyCall(changeInfo: JetChangeInfo, element: KtCallElement) {
            val newReceiverInfo = changeInfo.receiverParameterInfo
            val originalReceiverInfo = changeInfo.methodDescriptor.receiver
            if (newReceiverInfo == originalReceiverInfo) return

            val arguments = element.valueArgumentList.sure { "Argument list is expected: " + element.text }
            val oldArguments = element.valueArguments

            val psiFactory = KtPsiFactory(element.project)

            val firstArgument = oldArguments.firstOrNull() as KtValueArgument?

            when {
                newReceiverInfo != null -> {
                    val defaultValueForCall = newReceiverInfo.defaultValueForCall ?: psiFactory.createExpression("_")
                    val newReceiverArgument = psiFactory.createArgument(defaultValueForCall, null, false)

                    if (originalReceiverInfo != null) {
                        firstArgument?.replace(newReceiverArgument)
                    }
                    else {
                        arguments.addArgumentAfter(newReceiverArgument, null)
                    }
                }

                firstArgument != null -> arguments.removeArgument(firstArgument)
            }
        }

        private fun getReceiverExpression(receiver: ReceiverValue, psiFactory: KtPsiFactory): KtExpression? {
            return when (receiver) {
                is ExpressionReceiver -> receiver.expression
                is ThisReceiver -> {
                    val descriptor = receiver.declarationDescriptor
                    val thisText = if (descriptor is ClassDescriptor) "this@" + descriptor.name.asString() else "this"
                    psiFactory.createExpression(thisText)
                }
                else -> null
            }
        }

        private fun getParamIndexToArgumentMap(changeInfo: JetChangeInfo, oldArguments: List<ValueArgument>): MutableMap<Int, ValueArgument> {
            val argumentMap = HashMap<Int, ValueArgument>()

            for (i in oldArguments.indices) {
                val argument = oldArguments[i]
                val argumentName = argument.getArgumentName()
                val oldParameterName = if (argumentName != null) argumentName.asName.asString() else null

                if (oldParameterName != null) {
                    val oldParameterIndex = changeInfo.getOldParameterIndex(oldParameterName)

                    if (oldParameterIndex != null)
                        argumentMap.put(oldParameterIndex, argument)
                }
                else
                    argumentMap.put(i, argument)
            }

            return argumentMap
        }
    }
}
