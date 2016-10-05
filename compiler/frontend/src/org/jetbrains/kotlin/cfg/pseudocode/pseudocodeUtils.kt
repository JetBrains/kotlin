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

package org.jetbrains.kotlin.cfg.pseudocode

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cfg.Label
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind.*
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.AbstractJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ReturnValueInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ThrowExceptionInstruction
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.bindingContextUtil.getTargetFunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.ValueArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.findTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
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
            val rootCallableDescriptors = callableDescriptor.findTopMostOverriddenDescriptors()
            return or(rootCallableDescriptors.mapNotNull {
                it.getDispatchReceiverParameter()?.getType()?.let { TypeUtils.makeNullableIfNeeded(it, resolvedCall.call.isSafeCall()) }?.getSubtypesPredicate()
            })
        }
    }

    return null
}

fun getExpectedTypePredicate(
        value: PseudoValue,
        bindingContext: BindingContext,
        builtIns: KotlinBuiltIns
): TypePredicate {
    val pseudocode = value.createdAt?.owner ?: return AllTypes
    val typePredicates = LinkedHashSet<TypePredicate?>()

    fun addSubtypesOf(jetType: KotlinType?) = typePredicates.add(jetType?.getSubtypesPredicate())

    fun addByExplicitReceiver(resolvedCall: ResolvedCall<*>?) {
        val receiverValue = (resolvedCall ?: return).getExplicitReceiverValue()
        if (receiverValue != null) typePredicates.add(getReceiverTypePredicate(resolvedCall, receiverValue))
    }

    fun getTypePredicateForUnresolvedCallArgument(to: KtElement, inputValueIndex: Int): TypePredicate? {
        if (inputValueIndex < 0) return null
        val call = to.getCall(bindingContext) ?: return null
        val callee = call.getCalleeExpression() ?: return null

        val candidates = callee.getReferenceTargets(bindingContext)
                .filterIsInstance<FunctionDescriptor>()
                .sortedBy { DescriptorRenderer.DEBUG_TEXT.render(it) }
        if (candidates.isEmpty()) return null

        val explicitReceiver = call.getExplicitReceiver()
        val argValueOffset = if (explicitReceiver != null) 1 else 0

        val predicates = ArrayList<TypePredicate>()

        for (candidate in candidates) {
            val resolutionCandidate = ResolutionCandidate.create(
                    call,
                    candidate,
                    null,
                    ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                    null
            )
            val candidateCall = ResolvedCallImpl.create(
                    resolutionCandidate,
                    DelegatingBindingTrace(bindingContext, "Compute type predicates for unresolved call arguments"),
                    TracingStrategy.EMPTY,
                    DataFlowInfoForArgumentsImpl(DataFlowInfo.EMPTY, call)
            )
            val status = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(call,
                                                                                        TracingStrategy.EMPTY,
                                                                                        candidateCall,
                                                                                        LinkedHashSet())
            if (!status.isSuccess()) continue

            val candidateArgumentMap = candidateCall.getValueArguments()
            val callArguments = call.getValueArguments()
            val i = inputValueIndex - argValueOffset
            if (i < 0 || i >= callArguments.size) continue

            val mapping = candidateCall.getArgumentMapping(callArguments.get(i))
            if (mapping !is ArgumentMatch) continue

            val candidateParameter = mapping.valueParameter
            val resolvedArgument = candidateArgumentMap.get(candidateParameter)
            val expectedType = if (resolvedArgument is VarargValueArgument)
                candidateParameter.varargElementType
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
                        is KtReturnExpression -> returnElement.getTargetFunctionDescriptor(bindingContext)
                        else -> bindingContext[DECLARATION_TO_DESCRIPTOR, pseudocode.correspondingElement]
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
                                (accessTarget.resolvedCall.resultingDescriptor as? VariableDescriptor)?.type
                            is AccessTarget.Declaration ->
                                accessTarget.descriptor.type
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
                                    parameter.varargElementType
                                else ->
                                    parameter.getType()
                            }
                            addSubtypesOf(expectedType)
                        }
                    }
                }

                is MagicInstruction -> @Suppress("NON_EXHAUSTIVE_WHEN") when (it.kind) {
                    AND, OR ->
                        addSubtypesOf(builtIns.getBooleanType())

                    LOOP_RANGE_ITERATION ->
                        addByExplicitReceiver(bindingContext[LOOP_RANGE_ITERATOR_RESOLVED_CALL, value.element as? KtExpression])

                    VALUE_CONSUMER -> {
                        val element = it.element
                        when {
                            element.getStrictParentOfType<KtWhileExpression>()?.getCondition() == element ->
                                addSubtypesOf(builtIns.getBooleanType())

                            element is KtProperty -> {
                                val propertyDescriptor = bindingContext[DECLARATION_TO_DESCRIPTOR, element] as? PropertyDescriptor
                                propertyDescriptor?.getAccessors()?.map {
                                    addByExplicitReceiver(bindingContext[DELEGATED_PROPERTY_RESOLVED_CALL, it])
                                }
                            }

                            element is KtDelegatedSuperTypeEntry ->
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

fun Instruction.getPrimaryDeclarationDescriptorIfAny(bindingContext: BindingContext): DeclarationDescriptor? {
    return when (this) {
        is CallInstruction -> return resolvedCall.resultingDescriptor
        else -> PseudocodeUtil.extractVariableDescriptorIfAny(this, bindingContext)
    }
}

val Instruction.sideEffectFree: Boolean
    get() = owner.isSideEffectFree(this)

fun Instruction.calcSideEffectFree(): Boolean {
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
                    is KtNamedFunction -> element.name == null
                    is KtConstantExpression, is KtLambdaExpression, is KtStringTemplateExpression -> true
                    else -> false
                }
            }
        }

        is MagicInstruction -> kind.sideEffectFree

        else -> false
    }
}

fun Pseudocode.getElementValuesRecursively(element: KtElement): List<PseudoValue> {
    val results = ArrayList<PseudoValue>()

    fun Pseudocode.collectValues() {
        getElementValue(element)?.let { results.add(it) }
        for (localFunction in localDeclarations) {
            localFunction.body.collectValues()
        }
    }

    collectValues()
    return results
}

val KtElement.containingDeclarationForPseudocode: KtDeclaration?
        get() = PsiTreeUtil.getParentOfType(this, KtDeclarationWithBody::class.java, KtClassOrObject::class.java, KtScript::class.java)
                ?: getNonStrictParentOfType<KtProperty>()

fun KtDeclaration.getContainingPseudocode(context: BindingContext): Pseudocode? {
    val enclosingPseudocodeDeclaration = (this as? KtFunctionLiteral)?.let {
        it.parents.firstOrNull { it is KtDeclaration && it !is KtFunctionLiteral } as? KtDeclaration
    } ?: this

    val enclosingPseudocode = PseudocodeUtil.generatePseudocode(enclosingPseudocodeDeclaration, context)
    return enclosingPseudocode.getPseudocodeByElement(this)
}

fun KtElement.getContainingPseudocode(context: BindingContext) = containingDeclarationForPseudocode?.getContainingPseudocode(context)

fun Pseudocode.getPseudocodeByElement(element: KtElement): Pseudocode? {
    if (correspondingElement == element) return this

    localDeclarations.forEach { decl -> decl.body.getPseudocodeByElement(element)?.let { return it } }
    return null
}

val Label.isJumpToError: Boolean
    get() = resolveToInstruction() == pseudocode.errorInstruction
