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

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.idea.core.refactoring.replaceListPsiAndKeepDelimiters
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.isInsideOfCallerBody
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.createNameCounterpartMap
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.sure
import java.util.*

class KotlinFunctionCallUsage(
        element: KtCallElement,
        private val callee: KotlinCallableDefinitionUsage<*>
) : KotlinUsageInfo<KtCallElement>(element) {
    private val context = element.analyze(BodyResolveMode.FULL)
    private val resolvedCall = element.getResolvedCall(context)

    override fun processUsage(changeInfo: KotlinChangeInfo, element: KtCallElement, allUsages: Array<out UsageInfo>): Boolean {
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

    protected fun changeNameIfNeeded(changeInfo: KotlinChangeInfo, element: KtCallElement) {
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

    class ArgumentInfo(
            val parameter: KotlinParameterInfo,
            val parameterIndex: Int,
            val resolvedArgument: ResolvedValueArgument?,
            val receiverValue: ReceiverValue?
    ) {
        val mainValueArgument: ValueArgument?
            get() = resolvedArgument?.arguments?.firstOrNull()

        val wasNamed: Boolean
            get() = mainValueArgument?.isNamed() ?: false

        var name: String? = null
            private set

        fun makeNamed(callee: KotlinCallableDefinitionUsage<*>) {
            name = parameter.getInheritedName(callee)
        }

        fun shouldSkip() = parameter.defaultValueForParameter != null && mainValueArgument == null
    }

    private fun getResolvedValueArgument(oldIndex: Int): ResolvedValueArgument? {
        if (oldIndex < 0) return null

        val parameterDescriptor = resolvedCall!!.resultingDescriptor.valueParameters[oldIndex]
        return resolvedCall.valueArguments[parameterDescriptor]
    }

    private var KtValueArgument.generatedArgumentValue: Boolean
            by NotNullableCopyableUserDataProperty(Key.create("GENERATED_ARGUMENT_VALUE"), false)

    private fun ArgumentInfo.getArgumentByDefaultValue(
            element: KtCallElement,
            allUsages: Array<out UsageInfo>,
            psiFactory: KtPsiFactory
    ): KtValueArgument {
        val isInsideOfCallerBody = element.isInsideOfCallerBody(allUsages)
        val defaultValueForCall = parameter.defaultValueForCall
        val argValue = when {
            isInsideOfCallerBody -> psiFactory.createExpression(parameter.name)
            defaultValueForCall != null -> substituteReferences(defaultValueForCall, parameter.defaultValueParameterReferences, psiFactory)
            else -> null
        }
        val argName = (if (isInsideOfCallerBody) null else name)?.let { Name.guess(it) }
        return psiFactory.createArgument(argValue ?: psiFactory.createExpression("0"), argName).apply {
            generatedArgumentValue = true
            if (argValue == null) {
                getArgumentExpression()!!.delete()
            }
        }
    }

    private fun updateArgumentsAndReceiver(changeInfo: KotlinChangeInfo, element: KtCallElement, allUsages: Array<out UsageInfo>) {
        if (isPropertyJavaUsage) return updateJavaPropertyCall(changeInfo, element)

        val fullCallElement = element.getQualifiedExpressionForSelector() ?: element

        val oldArguments = element.valueArguments
        val newParameters = changeInfo.getNonReceiverParameters()

        val purelyNamedCall = element is KtCallExpression && oldArguments.isNotEmpty() && oldArguments.all { it.isNamed() }

        val newReceiverInfo = changeInfo.receiverParameterInfo
        val originalReceiverInfo = changeInfo.methodDescriptor.receiver

        val extensionReceiver = if (resolvedCall != null) resolvedCall.extensionReceiver else ReceiverValue.NO_RECEIVER
        val dispatchReceiver = if (resolvedCall != null) resolvedCall.dispatchReceiver else ReceiverValue.NO_RECEIVER

        // Do not add extension receiver to calls with explicit dispatch receiver
        if (newReceiverInfo != null && fullCallElement is KtQualifiedExpression && dispatchReceiver is ExpressionReceiver) return

        val newArgumentInfos = newParameters.withIndex().map {
            val (index, param) = it
            val oldIndex = param.oldIndex
            val resolvedArgument = if (oldIndex >= 0) getResolvedValueArgument(oldIndex) else null
            val receiverValue = if (param == originalReceiverInfo) extensionReceiver else null
            ArgumentInfo(param, index, resolvedArgument, receiverValue)
        }

        val lastParameterIndex = newParameters.lastIndex
        var firstNamedIndex = newArgumentInfos.firstOrNull {
            it.wasNamed
            || (it.parameter.isNewParameter && purelyNamedCall)
            || (it.resolvedArgument is VarargValueArgument && it.parameterIndex < lastParameterIndex)
        }?.parameterIndex
        if (firstNamedIndex == null) {
            val lastNonDefaultArgIndex = (lastParameterIndex downTo 0).firstOrNull { !newArgumentInfos[it].shouldSkip() }
                                         ?: -1
            firstNamedIndex = (0..lastNonDefaultArgIndex).firstOrNull { newArgumentInfos[it].shouldSkip() }
        }

        val lastPositionalIndex = if (firstNamedIndex != null) firstNamedIndex - 1 else lastParameterIndex
        (lastPositionalIndex + 1 .. lastParameterIndex).forEach { newArgumentInfos[it].makeNamed(callee) }

        val psiFactory = KtPsiFactory(element.project)

        val newArgumentList = psiFactory.createCallArguments("()").apply {
            for (argInfo in newArgumentInfos) {
                if (argInfo.shouldSkip()) continue

                val name = argInfo.name?.let { Name.guess(it) }

                if (argInfo.receiverValue != null) {
                    val receiverExpression = getReceiverExpression(argInfo.receiverValue, psiFactory) ?: continue
                    addArgument(psiFactory.createArgument(receiverExpression, name))
                    continue
                }

                val resolvedArgument = argInfo.resolvedArgument
                when (resolvedArgument) {
                    null, is DefaultValueArgument -> addArgument(argInfo.getArgumentByDefaultValue(element, allUsages, psiFactory))

                    is ExpressionValueArgument -> {
                        val valueArgument = resolvedArgument.valueArgument
                        val newValueArgument: KtValueArgument = when {
                            valueArgument == null -> argInfo.getArgumentByDefaultValue(element, allUsages, psiFactory)
                            valueArgument is KtFunctionLiteralArgument -> psiFactory.createArgument(valueArgument.getArgumentExpression(), name)
                            valueArgument is KtValueArgument && valueArgument.getArgumentName()?.asName == name -> valueArgument
                            else -> psiFactory.createArgument(valueArgument.getArgumentExpression(), name)
                        }
                        addArgument(newValueArgument)
                    }

                    // TODO: Support Kotlin varargs
                    is VarargValueArgument -> resolvedArgument.arguments.forEach {
                        if (it is KtValueArgument) addArgument(it)
                    }

                    else -> return
                }
            }
        }

        newArgumentList.arguments.singleOrNull()?.let {
            if (it.getArgumentExpression() == null) {
                newArgumentList.removeArgument(it)
            }
        }

        val lastOldArgument = oldArguments.lastOrNull()
        val lastNewParameter = newParameters.lastOrNull()
        val lastNewArgument = newArgumentList.arguments.lastOrNull()
        val oldLastResolvedArgument = getResolvedValueArgument(lastNewParameter?.oldIndex ?: -1) as? ExpressionValueArgument
        val lambdaArgumentNotTouched =
                lastOldArgument is KtFunctionLiteralArgument && oldLastResolvedArgument?.valueArgument == lastOldArgument
        val newLambdaArgumentAddedLast = lastNewParameter != null
                                         && lastNewParameter.isNewParameter
                                         && lastNewParameter.defaultValueForCall is KtFunctionLiteralExpression
                                         && lastNewArgument != null
                                         && !lastNewArgument.isNamed()

        if (lambdaArgumentNotTouched) {
            newArgumentList.removeArgument(newArgumentList.arguments.last())
        }
        else {
            val lambdaArguments = element.functionLiteralArguments
            if (lambdaArguments.isNotEmpty()) {
                element.deleteChildRange(lambdaArguments.first(), lambdaArguments.last())
            }
        }

        var oldArgumentList = element.valueArgumentList.sure { "Argument list is expected: " + element.text }
        replaceListPsiAndKeepDelimiters(oldArgumentList, newArgumentList) { arguments }

        element.accept(
                object: KtTreeVisitorVoid() {
                    override fun visitArgument(argument: KtValueArgument) {
                        if (argument.generatedArgumentValue) {
                            argument.generatedArgumentValue = false
                            argument.addToShorteningWaitSet(SHORTEN_ARGUMENTS_OPTIONS)
                        }
                    }
                }
        )

        var newElement: KtElement = element
        if (newReceiverInfo != originalReceiverInfo) {
            val replacingElement: PsiElement
            if (newReceiverInfo != null) {
                val receiverArgument = getResolvedValueArgument(newReceiverInfo.oldIndex)?.arguments?.singleOrNull()
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

            newElement = fullCallElement.replace(replacingElement) as KtElement
        }

        if (!lambdaArgumentNotTouched && newLambdaArgumentAddedLast) {
            val newCallExpression = ((newElement as? KtQualifiedExpression)?.selectorExpression ?: newElement) as KtCallExpression
            newCallExpression.moveFunctionLiteralOutsideParentheses()
        }
    }

    private fun changeArgumentNames(changeInfo: KotlinChangeInfo, element: KtCallElement) {
        for (argument in element.valueArguments) {
            val argumentName = argument.getArgumentName()
            val argumentNameExpression = argumentName?.referenceExpression ?: continue
            val oldParameterIndex = changeInfo.getOldParameterIndex(argumentNameExpression.getReferencedName()) ?: continue
            val newParameterIndex = if (changeInfo.receiverParameterInfo != null) oldParameterIndex + 1 else oldParameterIndex
            val parameterInfo = changeInfo.newParameters[newParameterIndex]
            changeArgumentName(argumentNameExpression, parameterInfo)
        }
    }

    private fun changeArgumentName(argumentNameExpression: KtSimpleNameExpression?, parameterInfo: KotlinParameterInfo) {
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

        private fun updateJavaPropertyCall(changeInfo: KotlinChangeInfo, element: KtCallElement) {
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
    }
}
