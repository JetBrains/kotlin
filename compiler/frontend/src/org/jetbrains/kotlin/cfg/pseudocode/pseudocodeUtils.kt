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

package org.jetbrains.kotlin.cfg.pseudocode

import com.google.common.collect.Sets
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartFMap
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ThrowExceptionInstruction
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.ValueArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

fun getReceiverTypePredicate(resolvedCall: ResolvedCall<*>, receiverValue: ReceiverValue): TypePredicate? {
    val callableDescriptor = resolvedCall.getResultingDescriptor() ?: return null

    when (receiverValue) {
        resolvedCall.getExtensionReceiver() -> {
            val receiverParameter = callableDescriptor.getExtensionReceiverParameter()
            if (receiverParameter != null) return receiverParameter.getType().getSubtypesPredicate()
        }
        resolvedCall.getDispatchReceiver() -> {
            val rootCallableDescriptors = OverridingUtil.getTopmostOverridenDescriptors(callableDescriptor)
            return or(rootCallableDescriptors.map {
                it.getDispatchReceiverParameter()?.getType()?.let { TypeUtils.makeNullableIfNeeded(it, resolvedCall.isSafeCall()) }?.getSubtypesPredicate()
            }.filterNotNull())
        }
    }

    return null
}

public fun getExpectedTypePredicate(value: PseudoValue, bindingContext: BindingContext): TypePredicate {
    val pseudocode = value.createdAt?.owner ?: return AllTypes
    val builtIns = KotlinBuiltIns.getInstance()

    val typePredicates = LinkedHashSet<TypePredicate?>()

    fun addSubtypesOf(jetType: JetType?) = typePredicates.add(jetType?.getSubtypesPredicate())

    fun addByExplicitReceiver(resolvedCall: ResolvedCall<*>?) {
        val receiverValue = (resolvedCall ?: return).getExplicitReceiverValue()
        if (receiverValue.exists()) typePredicates.add(getReceiverTypePredicate(resolvedCall, receiverValue))
    }

    fun getTypePredicateForUnresolvedCallArgument(to: JetElement, inputValueIndex: Int): TypePredicate? {
        if (inputValueIndex < 0) return null
        val call = to.getCall(bindingContext) ?: return null
        val callee = call.getCalleeExpression() ?: return null

        val candidates = callee.getReferenceTargets(bindingContext)
                .filterIsInstance<FunctionDescriptor>()
                .sortBy { DescriptorRenderer.DEBUG_TEXT.render(it) }
        if (candidates.isEmpty()) return null

        val explicitReceiver = call.getExplicitReceiver()
        val argValueOffset = if (explicitReceiver.exists()) 1 else 0

        val predicates = ArrayList<TypePredicate>()

        for (candidate in candidates) {
            val resolutionCandidate = ResolutionCandidate.create(
                    call,
                    candidate,
                    call.getDispatchReceiver(),
                    explicitReceiver,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    null
            )
            val candidateCall = ResolvedCallImpl.create(
                    resolutionCandidate,
                    DelegatingBindingTrace(bindingContext, "Compute type predicates for unresolved call arguments"),
                    TracingStrategy.EMPTY,
                    DataFlowInfoForArgumentsImpl(call)
            )
            val status = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(call,
                                                                                        TracingStrategy.EMPTY,
                                                                                        candidateCall,
                                                                                        LinkedHashSet())
            if (!status.isSuccess()) continue

            val candidateArgumentMap = candidateCall.getValueArguments()
            val callArguments = call.getValueArguments()
            val i = inputValueIndex - argValueOffset
            if (i < 0 || i >= callArguments.size()) continue

            val mapping = candidateCall.getArgumentMapping(callArguments.get(i))
            if (mapping !is ArgumentMatch) continue

            val candidateParameter = mapping.valueParameter
            val resolvedArgument = candidateArgumentMap.get(candidateParameter)
            val expectedType = if (resolvedArgument is VarargValueArgument)
                candidateParameter.getVarargElementType()
            else
                candidateParameter.getType()

            predicates.add(if (expectedType != null) AllSubtypes(expectedType) else AllTypes)
        }

        return or(predicates)
    }

    fun addTypePredicates(value: PseudoValue) {
        pseudocode.getUsages(value).forEach {
            when (it) {
                is ReturnValueInstruction -> {
                    val returnElement = it.element
                    val functionDescriptor = when(returnElement) {
                        is JetReturnExpression -> returnElement.getTargetFunctionDescriptor(bindingContext)
                        else -> bindingContext[DECLARATION_TO_DESCRIPTOR, pseudocode.getCorrespondingElement()]
                    }
                    addSubtypesOf((functionDescriptor as? CallableDescriptor)?.getReturnType())
                }

                is ConditionalJumpInstruction ->
                    addSubtypesOf(builtIns.getBooleanType())

                is ThrowExceptionInstruction ->
                    addSubtypesOf(builtIns.getThrowable().getDefaultType())

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

                is MagicInstruction -> @suppress("NON_EXHAUSTIVE_WHEN") when (it.kind) {
                    AND, OR ->
                        addSubtypesOf(builtIns.getBooleanType())

                    LOOP_RANGE_ITERATION ->
                        addByExplicitReceiver(bindingContext[LOOP_RANGE_ITERATOR_RESOLVED_CALL, value.element as? JetExpression])

                    VALUE_CONSUMER -> {
                        val element = it.element
                        when {
                            element.getStrictParentOfType<JetWhileExpression>()?.getCondition() == element ->
                                addSubtypesOf(builtIns.getBooleanType())

                            element is JetProperty -> {
                                val propertyDescriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, element] as? PropertyDescriptor
                                propertyDescriptor?.getAccessors()?.map {
                                    addByExplicitReceiver(bindingContext[DELEGATED_PROPERTY_RESOLVED_CALL, it])
                                }
                            }

                            element is JetDelegatorByExpressionSpecifier ->
                                addSubtypesOf(bindingContext[TYPE, element.getTypeReference()])
                        }
                    }

                    UNRESOLVED_CALL -> {
                        val typePredicate = getTypePredicateForUnresolvedCallArgument(it.element, it.inputValues.indexOf(value))
                        typePredicates.add(typePredicate)
                    }
                }
            }
        }
    }

    addTypePredicates(value)
    return and(typePredicates.filterNotNull())
}

public fun Instruction.getPrimaryDeclarationDescriptorIfAny(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is CallInstruction -> return resolvedCall.getResultingDescriptor()
        else -> PseudocodeUtil.extractVariableDescriptorIfAny(this, false, bindingContext)
    }
}

public val Instruction.sideEffectFree: Boolean
    get() = owner.isSideEffectFree(this)

private fun Instruction.calcSideEffectFree(): Boolean {
    if (this !is InstructionWithValue) return false
    if (!inputValues.all { it.createdAt?.sideEffectFree ?: false }) return false

    return when (this) {
        is ReadValueInstruction -> target.let {
            when (it) {
                is AccessTarget.Call -> when (it.resolvedCall.getResultingDescriptor()) {
                    is LocalVariableDescriptor, is ValueParameterDescriptor, is ReceiverParameterDescriptor -> true
                    else -> false
                }

                else -> when (element) {
                    is JetConstantExpression, is JetFunctionLiteralExpression, is JetStringTemplateExpression -> true
                    else -> false
                }
            }
        }

        is MagicInstruction -> kind.sideEffectFree

        else -> false
    }
}

fun Pseudocode.getElementValuesRecursively(element: JetElement): List<PseudoValue> {
    val results = ArrayList<PseudoValue>()

    fun Pseudocode.collectValues() {
        getElementValue(element)?.let { results.add(it) }
        for (localFunction in getLocalDeclarations()) {
            localFunction.body.collectValues()
        }
    }

    collectValues()
    return results
}

public fun JetElement.getContainingPseudocode(context: BindingContext): Pseudocode? {
    val pseudocodeDeclaration =
            PsiTreeUtil.getParentOfType(this, javaClass<JetDeclarationWithBody>(), javaClass<JetClassOrObject>())
            ?: getNonStrictParentOfType<JetProperty>()
            ?: return null

    val enclosingPseudocodeDeclaration = (pseudocodeDeclaration as? JetFunctionLiteral)?.let {
        it.parents.firstOrNull { it is JetDeclaration && it !is JetFunctionLiteral } as? JetDeclaration
    } ?: pseudocodeDeclaration

    val enclosingPseudocode = PseudocodeUtil.generatePseudocode(enclosingPseudocodeDeclaration, context)
    return enclosingPseudocode.getPseudocodeByElement(pseudocodeDeclaration)
           ?: throw AssertionError("Can't find nested pseudocode for element: ${pseudocodeDeclaration.getElementTextWithContext()}")
}

public fun Pseudocode.getPseudocodeByElement(element: JetElement): Pseudocode? {
    if (getCorrespondingElement() == element) return this

    getLocalDeclarations().forEach { decl -> decl.body.getPseudocodeByElement(element)?.let { return it } }
    return null
}