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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.*
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.*
import org.jetbrains.jet.lang.resolve.calls.model.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.BindingContext
import java.util.*
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.JetType

fun JetExpression.isStatement(pseudocode: Pseudocode): Boolean {
    val value = pseudocode.getElementValue(this);
    if (value == null) return true

    fun considerUsedIfCreatedBeforeExit(): Boolean {
        return when {
            (getParent() as? JetFunction)?.getBodyExpression() == this ->
                true
            value.implicitReturnValue ->
                true
            else ->
                false
        }
    }

    val instruction = value.createdAt
    if (considerUsedIfCreatedBeforeExit() && instruction.nextInstructions.any { it == pseudocode.getExitInstruction() }) return false
    return traverseFollowingInstructions(instruction, HashSet(), TraversalOrder.FORWARD) { value !in it.inputValues }
}

val PseudoValue.implicitReturnValue: Boolean
    get() {
        val pseudocode = createdAt.owner

        val function = pseudocode.getCorrespondingElement() as? JetDeclarationWithBody

        if (function is JetFunctionLiteral || (function != null && !function.hasBlockBody())) {
            return pseudocode.getElementValue(function.getBodyExpression()) == this
        }
        return false
    }

fun Pseudocode.collectValueUsages(): Map<PseudoValue, List<Instruction>> {
    val map = HashMap<PseudoValue, MutableList<Instruction>>()
    traverseFollowingInstructions(getEnterInstruction(), HashSet(), TraversalOrder.FORWARD) {
        for (value in it.inputValues) {
            map.getOrPut(value){ ArrayList() }.add(it)
        }
        true
    }

    return map
}

fun getReceiverTypePredicate(resolvedCall: ResolvedCall<*>, receiverValue: ReceiverValue): TypePredicate? {
    val callableDescriptor = resolvedCall.getResultingDescriptor()
    if (callableDescriptor == null) return null

    when (receiverValue) {
        resolvedCall.getReceiverArgument() -> {
            val receiverParameter = callableDescriptor.getReceiverParameter()
            if (receiverParameter != null) return receiverParameter.getType().getSubtypesPredicate()
        }
        resolvedCall.getThisObject() -> {
            val rootCallableDescriptors = OverridingUtil.getTopmostOverridenDescriptors(callableDescriptor)
            return or(rootCallableDescriptors.map {
                it.getExpectedThisObject()?.getType()?.let { TypeUtils.makeNullableIfNeeded(it, resolvedCall.isSafeCall()) }?.getSubtypesPredicate()
            }.filterNotNull())
        }
    }

    return null
}

fun getExpectedTypePredicate(
        value: PseudoValue,
        valueUsageMap: Map<PseudoValue, List<Instruction>>,
        bindingContext: BindingContext
): TypePredicate {
    val typePredicates = HashSet<TypePredicate?>()

    fun addSubtypesOf(jetType: JetType?) = typePredicates.add(jetType?.getSubtypesPredicate())

    fun addTypePredicates(value: PseudoValue) {
        if (value.implicitReturnValue) {
            val function = value.createdAt.owner.getCorrespondingElement() as? JetDeclarationWithBody
            val functionDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, function] as? FunctionDescriptor
            addSubtypesOf(functionDescriptor?.getReturnType())
        }

        valueUsageMap[value]?.forEach {
            when (it) {
                is ReturnValueInstruction -> {
                    val functionDescriptor = (it.element as JetReturnExpression).getTargetFunctionDescriptor(bindingContext)
                    addSubtypesOf(functionDescriptor?.getReturnType())
                }

                is ConditionalJumpInstruction ->
                    addSubtypesOf(KotlinBuiltIns.getInstance().getBooleanType())

                is ThrowExceptionInstruction ->
                    addSubtypesOf(KotlinBuiltIns.getInstance().getThrowable().getDefaultType())

                is MergeInstruction ->
                    addTypePredicates(it.outputValue)

                is AccessValueInstruction -> {
                    val accessTarget = it.target
                    val receiverValue = it.receiverValues[value]
                    if (receiverValue != null) {
                        typePredicates.add(getReceiverTypePredicate((accessTarget as AccessTarget.Call).resolvedCall, receiverValue))
                    }
                    else {
                        val expectedType = when (accessTarget) {
                            is AccessTarget.Call ->
                                (accessTarget.resolvedCall.getResultingDescriptor() as? VariableDescriptor)?.getType()
                            is AccessTarget.Declaration ->
                                accessTarget.descriptor.getType()
                            else ->
                                null
                        }
                        addSubtypesOf(expectedType)
                    }
                }

                is CallInstruction -> {
                    val receiverValue = it.receiverValues[value]
                    if (receiverValue != null) {
                        typePredicates.add(getReceiverTypePredicate(it.resolvedCall, receiverValue))
                    }
                    else {
                        it.arguments[value]?.let { parameter ->
                            val expectedType = when (it.resolvedCall.getValueArguments()[parameter]) {
                                is VarargValueArgument ->
                                    parameter.getVarargElementType()
                                else ->
                                    parameter.getType()
                            }
                            addSubtypesOf(expectedType)
                        }
                    }
                }

                is MagicInstruction ->
                    typePredicates.add(it.expectedTypes[value])
            }
        }
    }

    addTypePredicates(value)
    return and(typePredicates.filterNotNull())
}