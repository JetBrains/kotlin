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

package org.jetbrains.kotlin.idea.util.psi.patternMatching

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.*
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.Status.*
import java.util.HashMap
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPsiUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.JetType
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import java.util.Collections
import org.jetbrains.kotlin.psi.JetReferenceExpression
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.psi.JetIfExpression
import org.jetbrains.kotlin.psi.JetUnaryExpression
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetConstantExpression
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.psi.JetLabelReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.types.ErrorUtils
import com.intellij.lang.ASTNode
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.JetCallableReferenceExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.psi.JetThisExpression
import org.jetbrains.kotlin.psi.JetStringTemplateEntryWithExpression
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange.Empty
import org.jetbrains.kotlin.psi.JetMultiDeclaration
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.psi.JetWithExpressionInitializer
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetTypeParameter
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetClassInitializer
import org.jetbrains.kotlin.psi.JetTypeParameterListOwner
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.psi.JetReturnExpression
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.psi.JetDelegatorToSuperClass
import org.jetbrains.kotlin.psi.JetDelegationSpecifier
import org.jetbrains.kotlin.idea.refactoring.getContextForContainingDeclarationBody
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetOperationReferenceExpression

public trait UnificationResult {
    public enum class Status {
        MATCHED {
            override fun and(other: Status): Status = other
        }

        UNMATCHED {
            override fun and(other: Status): Status = this
        }

        public abstract fun and(other: Status): Status
    }

    object Unmatched : UnificationResult {
        override val status: Status get() = UNMATCHED
    }

    trait Matched: UnificationResult {
        val substitution: Map<UnifierParameter, JetExpression>
        override val status: Status get() = MATCHED
    }

    class StronglyMatched(override val substitution: Map<UnifierParameter, JetExpression>): Matched

    class WeaklyMatched(
            override val substitution: Map<UnifierParameter, JetExpression>,
            val weakMatches: Map<JetElement, JetElement>
    ): Matched

    val status: Status
    val matched: Boolean get() = status != UNMATCHED
}

public class UnifierParameter(
        val descriptor: DeclarationDescriptor,
        val expectedType: JetType
)

public class JetPsiUnifier(
        parameters: Collection<UnifierParameter> = Collections.emptySet(),
        val allowWeakMatches: Boolean = false
) {
    default object {
        val DEFAULT = JetPsiUnifier()
    }

    private inner class Context(
            val originalTarget: JetPsiRange,
            val originalPattern: JetPsiRange
    ) {
        val patternContext: BindingContext = originalPattern.getBindingContext()
        val targetContext: BindingContext = originalTarget.getBindingContext()
        val substitution = HashMap<UnifierParameter, JetExpression>()
        val declarationPatternsToTargets = MultiMap<DeclarationDescriptor, DeclarationDescriptor>()
        val weakMatches = HashMap<JetElement, JetElement>()
        var checkEquivalence: Boolean = false

        private fun JetPsiRange.getBindingContext(): BindingContext {
            val element = (this as? JetPsiRange.ListRange)?.startElement as? JetElement
            if ((element?.getContainingFile() as? JetFile)?.doNotAnalyze != null) return BindingContext.EMPTY
            return element?.getContextForContainingDeclarationBody() ?: BindingContext.EMPTY
        }

        private fun matchDescriptors(d1: DeclarationDescriptor?, d2: DeclarationDescriptor?): Boolean {
            if (d1 == d2 || d2 in declarationPatternsToTargets[d1] || d1 in declarationPatternsToTargets[d2]) return true
            if (d1 == null || d2 == null) return false

            val decl1 = DescriptorToSourceUtils.descriptorToDeclaration(d1) as? JetDeclaration
            val decl2 = DescriptorToSourceUtils.descriptorToDeclaration(d2) as? JetDeclaration
            if (decl1 == null || decl2 == null) return false

            if ((decl1 in originalTarget && decl2 in originalPattern) || (decl2 in originalTarget && decl1 in originalPattern)) {
                return matchDeclarations(decl1, decl2, d1, d2) == MATCHED
            }

            return false
        }

        private fun matchReceivers(rv1: ReceiverValue, rv2: ReceiverValue): Boolean {
            return when {
                rv1 is ExpressionReceiver && rv2 is ExpressionReceiver ->
                    doUnify(rv1.getExpression(), rv2.getExpression()) == MATCHED

                rv1 is ThisReceiver && rv2 is ThisReceiver ->
                    matchDescriptors(rv1.getDeclarationDescriptor(), rv2.getDeclarationDescriptor())

                else ->
                    rv1 == rv2
            }
        }

        private fun matchCalls(call1: Call, call2: Call): Boolean {
            return matchReceivers(call1.getExplicitReceiver(), call2.getExplicitReceiver()) &&
                   matchReceivers(call1.getDispatchReceiver(), call2.getDispatchReceiver())
        }

        private fun matchArguments(arg1: ValueArgument, arg2: ValueArgument): Status {
            return when {
                arg1.isExternal() != arg2.isExternal() ->
                    UNMATCHED

                (arg1.getSpreadElement() == null) != (arg2.getSpreadElement() == null) ->
                    UNMATCHED

                else ->
                    doUnify(arg1.getArgumentExpression(), arg2.getArgumentExpression())
            }
        }

        private fun matchResolvedCalls(rc1: ResolvedCall<*>, rc2: ResolvedCall<*>): Status? {
            fun checkSpecialOperations(): Boolean {
                val op1 = (rc1.getCall().getCalleeExpression() as? JetSimpleNameExpression)?.getReferencedNameElementType()
                val op2 = (rc2.getCall().getCalleeExpression() as? JetSimpleNameExpression)?.getReferencedNameElementType()

                return when {
                    op1 == op2 ->
                        true
                    op1 == JetTokens.NOT_IN, op2 == JetTokens.NOT_IN ->
                        false
                    op1 == JetTokens.EXCLEQ, op2 == JetTokens.EXCLEQ ->
                        false
                    op1 in OperatorConventions.COMPARISON_OPERATIONS, op2 in OperatorConventions.COMPARISON_OPERATIONS ->
                        false
                    else ->
                        true
                }
            }

            fun checkArguments(): Status? {
                val args1 = rc1.getResultingDescriptor()?.getValueParameters()?.map { rc1.getValueArguments()[it] } ?: Collections.emptyList()
                val args2 = rc2.getResultingDescriptor()?.getValueParameters()?.map { rc2.getValueArguments()[it] } ?: Collections.emptyList()
                if (args1.size != args2.size) return UNMATCHED
                if (rc1.getCall().getValueArguments().size != args1.size || rc2.getCall().getValueArguments().size != args2.size) return null

                return (args1.stream() zip args2.stream()).fold(MATCHED) { (s, p) ->
                    val (arg1, arg2) = p
                    s and when {
                        arg1 == arg2 -> MATCHED
                        arg1 == null || arg2 == null -> UNMATCHED
                        else -> (arg1.getArguments().stream() zip arg2.getArguments().stream()).fold(MATCHED) { (s, p) ->
                            s and matchArguments(p.first, p.second)
                        }
                    }
                }
            }

            fun checkImplicitReceiver(implicitCall: ResolvedCall<*>, explicitCall: ResolvedCall<*>): Boolean {
                val (implicitReceiver, explicitReceiver) =
                        when (explicitCall.getExplicitReceiverKind()) {
                            ExplicitReceiverKind.EXTENSION_RECEIVER ->
                                (implicitCall.getExtensionReceiver() as? ThisReceiver) to
                                        (explicitCall.getExtensionReceiver() as? ExpressionReceiver)

                            ExplicitReceiverKind.DISPATCH_RECEIVER ->
                                (implicitCall.getDispatchReceiver() as? ThisReceiver) to
                                        (explicitCall.getDispatchReceiver() as? ExpressionReceiver)

                            else ->
                                null to null
                        }

                val thisExpression = explicitReceiver?.getExpression() as? JetThisExpression
                if (implicitReceiver == null || thisExpression == null) return false

                return matchDescriptors(
                        implicitReceiver.getDeclarationDescriptor(),
                        thisExpression.getAdjustedResolvedCall()?.getCandidateDescriptor()?.getContainingDeclaration()
                )
            }

            fun checkReceivers(): Boolean {
                return when {
                    rc1.getExplicitReceiverKind() == rc2.getExplicitReceiverKind() -> {
                        matchReceivers(rc1.getExtensionReceiver(), rc2.getExtensionReceiver()) &&
                        (rc1.getExplicitReceiverKind() == ExplicitReceiverKind.BOTH_RECEIVERS || matchReceivers(rc1.getDispatchReceiver(), rc2.getDispatchReceiver()))
                    }

                    rc1.getExplicitReceiverKind() == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> checkImplicitReceiver(rc1, rc2)

                    rc2.getExplicitReceiverKind() == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> checkImplicitReceiver(rc2, rc1)

                    else -> false
                }
            }

            fun checkTypeArguments(): Status? {
                val typeArgs1 = rc1.getTypeArguments().toList()
                val typeArgs2 = rc2.getTypeArguments().toList()
                if (typeArgs1.size() != typeArgs2.size()) return UNMATCHED

                for ((typeArg1, typeArg2) in (typeArgs1 zip typeArgs2)) {
                    if (!matchDescriptors(typeArg1.first, typeArg2.first)) return UNMATCHED

                    val s = matchTypes(typeArg1.second, typeArg2.second)
                    if (s != MATCHED) return s
                }

                return MATCHED
            }

            return when {
                !checkSpecialOperations() -> UNMATCHED
                !matchDescriptors(rc1.getCandidateDescriptor(), rc2.getCandidateDescriptor()) -> UNMATCHED
                !checkReceivers() -> UNMATCHED
                rc1.isSafeCall() != rc2.isSafeCall() -> UNMATCHED
                else -> {
                    val s = checkTypeArguments()
                    if (s != MATCHED) s else checkArguments()
                }
            }
        }

        private val JetElement.bindingContext: BindingContext get() = if (this in originalPattern) patternContext else targetContext

        private fun JetElement.getAdjustedResolvedCall(): ResolvedCall<*>? {
            val rc = getResolvedCall(bindingContext)?.let {
                when {
                    it !is VariableAsFunctionResolvedCall -> it
                    this is JetSimpleNameExpression -> it.variableCall
                    else -> it.functionCall
                }
            }

            return when {
                rc == null || ErrorUtils.isError(rc.getCandidateDescriptor()) -> null
                else -> rc
            }
        }

        private fun matchCalls(e1: JetElement, e2: JetElement): Status? {
            if (e1.shouldIgnoreResolvedCall() || e2.shouldIgnoreResolvedCall()) return null

            val resolvedCall1 = e1.getAdjustedResolvedCall()
            val resolvedCall2 = e2.getAdjustedResolvedCall()

            return when {
                resolvedCall1 != null && resolvedCall2 != null ->
                    matchResolvedCalls(resolvedCall1, resolvedCall2)

                resolvedCall1 == null && resolvedCall2 == null -> {
                    val call1 = e1.getCall(e1.bindingContext)
                    val call2 = e2.getCall(e2.bindingContext)

                    when {
                        call1 != null && call2 != null ->
                            if (matchCalls(call1, call2)) null else UNMATCHED

                        else ->
                            if (call1 == null && call2 == null) null else UNMATCHED
                    }
                }

                else ->
                    UNMATCHED
            }
        }

        private fun JetTypeReference.getType(): JetType? {
            val t = bindingContext[BindingContext.TYPE, this]
            return if (t == null || t.isError()) null else t
        }

        private fun matchTypes(type1: JetType?, type2: JetType?): Status? {
            if (type1 != null && type2 != null) {
                if (type1.isError() || type2.isError()) return null
                if (TypeUtils.equalTypes(type1, type2)) return MATCHED

                if (type1.isMarkedNullable() != type2.isMarkedNullable()) return UNMATCHED
                if (!matchDescriptors(
                        type1.getConstructor().getDeclarationDescriptor(),
                        type2.getConstructor().getDeclarationDescriptor())) return UNMATCHED

                val args1 = type1.getArguments()
                val args2 = type2.getArguments()
                if (args1.size != args2.size) return UNMATCHED
                if (!args1.zip(args2).all {
                    it.first.getProjectionKind() == it.second.getProjectionKind() && matchTypes(it.first.getType(), it.second.getType()) == MATCHED }
                ) return UNMATCHED

                return MATCHED
            }

            return if (type1 == null && type2 == null) null else UNMATCHED
        }

        private fun matchTypes(types1: Collection<JetType>, types2: Collection<JetType>): Boolean {
            fun sortTypes(types: Collection<JetType>) = types.sortBy{ DescriptorRenderer.DEBUG_TEXT.renderType(it) }

            if (types1.size != types2.size) return false
            return (sortTypes(types1) zip sortTypes(types2)).all { matchTypes(it.first, it.second) == MATCHED }
        }

        private fun JetElement.shouldIgnoreResolvedCall(): Boolean {
            return when {
                this is JetConstantExpression -> true
                this is JetOperationReferenceExpression -> getReferencedNameElementType() == JetTokens.EXCLEXCL
                this is JetIfExpression -> true
                this is JetUnaryExpression -> when (getOperationReference().getReferencedNameElementType()) {
                    JetTokens.EXCLEXCL, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS -> true
                    else -> false
                }
                this is JetBinaryExpression -> getOperationReference().getReferencedNameElementType() == JetTokens.ELVIS
                else -> false
            }
        }

        private fun JetBinaryExpression.matchComplexAssignmentWithSimple(simple: JetBinaryExpression): Status? {
            return when {
                doUnify(getLeft(), simple.getLeft()) == UNMATCHED -> UNMATCHED
                else -> simple.getRight()?.let { matchCalls(this, it) } ?: UNMATCHED
            }
        }

        private fun JetBinaryExpression.matchAssignment(e: JetElement): Status? {
            val operationType = getOperationReference().getReferencedNameElementType() as JetToken
            if (operationType == JetTokens.EQ) {
                if (e.shouldIgnoreResolvedCall()) return UNMATCHED

                if (JetPsiUtil.isAssignment(e) && !JetPsiUtil.isOrdinaryAssignment(e)) {
                    return (e as JetBinaryExpression).matchComplexAssignmentWithSimple(this)
                }

                val lhs = getLeft()?.unwrap()
                if (lhs !is JetArrayAccessExpression) return null

                val setResolvedCall = bindingContext[BindingContext.INDEXED_LVALUE_SET, lhs]
                val resolvedCallToMatch = e.getAdjustedResolvedCall()

                return if (setResolvedCall == null || resolvedCallToMatch == null) null else matchResolvedCalls(setResolvedCall, resolvedCallToMatch)
            }

            val assignResolvedCall = getAdjustedResolvedCall()
            if (assignResolvedCall == null) return UNMATCHED

            val operationName = OperatorConventions.getNameForOperationSymbol(operationType)
            if (assignResolvedCall.getResultingDescriptor()?.getName() == operationName) return matchCalls(this, e)

            return if (JetPsiUtil.isAssignment(e)) null else UNMATCHED
        }

        private fun matchLabelTargets(e1: JetLabelReferenceExpression, e2: JetLabelReferenceExpression): Status {
            val target1 = e1.bindingContext[BindingContext.LABEL_TARGET, e1]
            val target2 = e2.bindingContext[BindingContext.LABEL_TARGET, e2]

            return if (target1 == target2) MATCHED else UNMATCHED
        }

        private fun PsiElement.isIncrement(): Boolean {
            val parent = getParent()
            return parent is JetUnaryExpression
                   && this == parent.getOperationReference()
                   && ((parent.getOperationToken() as JetToken) in OperatorConventions.INCREMENT_OPERATIONS)
        }

        private fun matchCallableReferences(e1: JetCallableReferenceExpression, e2: JetCallableReferenceExpression): Boolean {
            val d1 = e1.bindingContext[BindingContext.REFERENCE_TARGET, e1.getCallableReference()]
            val d2 = e2.bindingContext[BindingContext.REFERENCE_TARGET, e2.getCallableReference()]
            return matchDescriptors(d1, d2)
        }

        private fun matchMultiDeclarations(e1: JetMultiDeclaration, e2: JetMultiDeclaration): Boolean {
            val entries1 = e1.getEntries()
            val entries2 = e2.getEntries()
            if (entries1.size != entries2.size) return false

            return entries1.zip(entries2).all { p ->
                val (entry1, entry2) = p
                val rc1 = entry1.bindingContext[BindingContext.COMPONENT_RESOLVED_CALL, entry1]
                val rc2 = entry2.bindingContext[BindingContext.COMPONENT_RESOLVED_CALL, entry2]
                when {
                    rc1 == null && rc2 == null -> true
                    rc1 != null && rc2 != null -> matchResolvedCalls(rc1, rc2) == MATCHED
                    else -> false
                }
            }
        }

        fun matchReceiverParameters(receiver1: ReceiverParameterDescriptor?, receiver2: ReceiverParameterDescriptor?): Boolean {
            val matchedReceivers = when {
                receiver1 == null && receiver2 == null -> true
                matchDescriptors(receiver1, receiver2) -> true
                receiver1 != null && receiver2 != null -> matchTypes(receiver1.getType(), receiver2.getType()) == MATCHED
                else -> false
            }

            if (matchedReceivers && receiver1 != null) {
                declarationPatternsToTargets.putValue(receiver1, receiver2)
            }

            return matchedReceivers
        }

        private fun matchCallables(
                decl1: JetDeclaration,
                decl2: JetDeclaration,
                desc1: CallableDescriptor,
                desc2: CallableDescriptor): Status? {
            fun needToCompareReturnTypes(): Boolean {
                if (decl1 !is JetCallableDeclaration) return true
                return decl1.getTypeReference() != null || (decl2 as JetCallableDeclaration).getTypeReference() != null
            }

            if (desc1 is VariableDescriptor && desc1.isVar() != (desc2 as VariableDescriptor).isVar()) return UNMATCHED

            if (!matchNames(decl1, decl2, desc1, desc2)) return UNMATCHED

            if (needToCompareReturnTypes()) {
                val type1 = desc1.getReturnType()
                val type2 = desc2.getReturnType()

                if (type1 != type2
                    && (type1 == null || type2 == null || type1.isError() || type2.isError() || matchTypes(type1, type2) != MATCHED)) {
                    return UNMATCHED
                }
            }

            if (!matchReceiverParameters(desc1.getExtensionReceiverParameter(), desc2.getExtensionReceiverParameter())) return UNMATCHED
            if (!matchReceiverParameters(desc1.getDispatchReceiverParameter(), desc2.getDispatchReceiverParameter())) return UNMATCHED

            val params1 = desc1.getValueParameters()
            val params2 = desc2.getValueParameters()
            val zippedParams = params1.zip(params2)
            val parametersMatch =
                    (params1.size == params2.size) && zippedParams.all { matchTypes(it.first.getType(), it.second.getType()) == MATCHED }
            if (!parametersMatch) return UNMATCHED

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }

            return doUnify(
                    (decl1 as? JetTypeParameterListOwner)?.getTypeParameters()?.toRange() ?: Empty,
                    (decl2 as? JetTypeParameterListOwner)?.getTypeParameters()?.toRange() ?: Empty
            ) and when (decl1) {
                is JetDeclarationWithBody ->
                    doUnify(decl1.getBodyExpression(), (decl2 as JetDeclarationWithBody).getBodyExpression())

                is JetWithExpressionInitializer ->
                    doUnify(decl1.getInitializer(), (decl2 as JetWithExpressionInitializer).getInitializer())

                is JetParameter ->
                    doUnify(decl1.getDefaultValue(), (decl2 as JetParameter).getDefaultValue())

                else ->
                    UNMATCHED
            }
        }

        private fun JetDeclaration.isNameRelevant(): Boolean {
            if (this is JetParameter && hasValOrVarNode()) return true

            val parent = getParent()
            return parent is JetClassBody || parent is JetFile
        }

        private fun matchNames(decl1: JetDeclaration, decl2: JetDeclaration, desc1: DeclarationDescriptor, desc2: DeclarationDescriptor): Boolean {
            return (!decl1.isNameRelevant() && !decl2.isNameRelevant()) || desc1.getName() == desc2.getName()
        }

        private fun matchContainedDescriptors<T: DeclarationDescriptor>(
                declarations1: List<T>,
                declarations2: List<T>,
                matchPair: (Pair<T, T>) -> Boolean
        ): Boolean {
            val zippedParams = declarations1 zip declarations2
            if (declarations1.size != declarations2.size || !zippedParams.all { matchPair(it) }) return false

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }
            return true
        }

        private fun matchClasses(
                decl1: JetClassOrObject,
                decl2: JetClassOrObject,
                desc1: ClassDescriptor,
                desc2: ClassDescriptor): Status? {
            class OrderInfo<T>(
                    val orderSensitive: List<T>,
                    val orderInsensitive: List<T>
            )

            fun getMemberOrderInfo(cls: JetClassOrObject): OrderInfo<JetDeclaration> {
                val (orderInsensitive, orderSensitive) = (cls.getBody()?.getDeclarations() ?: Collections.emptyList()).partition {
                    it is JetClassOrObject || it is JetFunction
                }

                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun getDelegationOrderInfo(cls: JetClassOrObject): OrderInfo<JetDelegationSpecifier> {
                val (orderInsensitive, orderSensitive) = cls.getDelegationSpecifiers().partition { it is JetDelegatorToSuperClass }
                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun resolveAndSortDeclarationsByDescriptor(declarations: List<JetDeclaration>): List<Pair<JetDeclaration, DeclarationDescriptor?>> {
                return declarations
                        .map { it to it.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] }
                        .sortBy { it.second?.let { IdeDescriptorRenderers.SOURCE_CODE.render(it) } ?: "" }
            }

            fun sortDeclarationsByElementType(declarations: List<JetDeclaration>): List<JetDeclaration> {
                return declarations.sortBy { it.getNode()?.getElementType()?.getIndex() ?: -1 }
            }

            if (desc1.getKind() != desc2.getKind()) return UNMATCHED
            if (!matchNames(decl1, decl2, desc1, desc2)) return UNMATCHED

            declarationPatternsToTargets.putValue(desc1.getThisAsReceiverParameter(), desc2.getThisAsReceiverParameter())

            val constructor1 = desc1.getUnsubstitutedPrimaryConstructor()
            val constructor2 = desc2.getUnsubstitutedPrimaryConstructor()
            if (constructor1 != null && constructor2 != null) {
                declarationPatternsToTargets.putValue(constructor1, constructor2)
            }

            val delegationInfo1 = getDelegationOrderInfo(decl1)
            val delegationInfo2 = getDelegationOrderInfo(decl2)

            if (delegationInfo1.orderInsensitive.size != delegationInfo2.orderInsensitive.size) return UNMATCHED
            @outer
            for (specifier1 in delegationInfo1.orderInsensitive) {
                for (specifier2 in delegationInfo2.orderInsensitive) {
                    if (doUnify(specifier1, specifier2) != UNMATCHED) continue @outer
                }
                return UNMATCHED
            }

            val status = doUnify((decl1 as? JetClass)?.getPrimaryConstructorParameterList(), (decl2 as? JetClass)?.getPrimaryConstructorParameterList()) and
                    doUnify((decl1 as? JetClass)?.getTypeParameterList(), (decl2 as? JetClass)?.getTypeParameterList()) and
                    doUnify(delegationInfo1.orderSensitive.toRange(), delegationInfo2.orderSensitive.toRange())
            if (status == UNMATCHED) return UNMATCHED

            val membersInfo1 = getMemberOrderInfo(decl1)
            val membersInfo2 = getMemberOrderInfo(decl2)

            val sortedMembers1 = resolveAndSortDeclarationsByDescriptor(membersInfo1.orderInsensitive)
            val sortedMembers2 = resolveAndSortDeclarationsByDescriptor(membersInfo2.orderInsensitive)
            if ((sortedMembers1.size != sortedMembers2.size)) return UNMATCHED
            if (sortedMembers1.zip(sortedMembers2).any {
                val (d1, d2) = it
                (matchDeclarations(d1.first, d2.first, d1.second, d2.second) ?: doUnify(d1.first, d2.first)) == UNMATCHED
            }) return UNMATCHED

            return doUnify(
                    sortDeclarationsByElementType(membersInfo1.orderSensitive).toRange(),
                    sortDeclarationsByElementType(membersInfo2.orderSensitive).toRange()
            )
        }

        private fun matchTypeParameters(
                desc1: TypeParameterDescriptor,
                desc2: TypeParameterDescriptor
        ): Status {
            if (desc1.getVariance() != desc2.getVariance()) return UNMATCHED
            if (!matchTypes(desc1.getLowerBounds(), desc2.getLowerBounds())) return UNMATCHED
            if (!matchTypes(desc1.getUpperBounds(), desc2.getUpperBounds())) return UNMATCHED
            return MATCHED
        }

        private fun JetDeclaration.matchDeclarations(e: PsiElement): Status? {
            if (e !is JetDeclaration) return UNMATCHED

            val desc1 = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
            val desc2 = e.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, e]
            return matchDeclarations(this, e, desc1, desc2)
        }

        private fun matchDeclarations(
                decl1: JetDeclaration,
                decl2: JetDeclaration,
                desc1: DeclarationDescriptor?,
                desc2: DeclarationDescriptor?): Status? {
            if (decl1.javaClass != decl2.javaClass) return UNMATCHED

            if (desc1 == null || desc2 == null || ErrorUtils.isError(desc1) || ErrorUtils.isError(desc2)) return UNMATCHED
            if (desc1.javaClass != desc2.javaClass) return UNMATCHED

            declarationPatternsToTargets.putValue(desc1, desc2)
            val status = when (decl1) {
                is JetDeclarationWithBody, is JetWithExpressionInitializer, is JetParameter ->
                    matchCallables(decl1, decl2, desc1 as CallableDescriptor, desc2 as CallableDescriptor)

                is JetClassOrObject ->
                    matchClasses(decl1, decl2 as JetClassOrObject, desc1 as ClassDescriptor, desc2 as ClassDescriptor)

                is JetTypeParameter ->
                    matchTypeParameters(desc1 as TypeParameterDescriptor, desc2 as TypeParameterDescriptor)

                else ->
                    null
            }
            if (status == UNMATCHED) {
                declarationPatternsToTargets.removeValue(desc1, desc2)
            }

            return status
        }

        private fun matchResolvedInfo(e1: PsiElement, e2: PsiElement): Status? {
            return when {
                e1 !is JetElement, e2 !is JetElement ->
                    null

                e1 is JetMultiDeclaration && e2 is JetMultiDeclaration ->
                    if (matchMultiDeclarations(e1, e2)) null else UNMATCHED

                e1 is JetClassInitializer && e2 is JetClassInitializer ->
                    null

                e1 is JetDeclaration ->
                    e1.matchDeclarations(e2)

                e2 is JetDeclaration ->
                    e2.matchDeclarations(e1)

                e1 is JetTypeReference && e2 is JetTypeReference ->
                    matchTypes(e1.getType(), e2.getType())

                JetPsiUtil.isAssignment(e1) ->
                    (e1 as JetBinaryExpression).matchAssignment(e2)

                JetPsiUtil.isAssignment(e2) ->
                    (e2 as JetBinaryExpression).matchAssignment(e1)

                e1 is JetLabelReferenceExpression && e2 is JetLabelReferenceExpression ->
                    matchLabelTargets(e1, e2)

                e1.isIncrement() != e2.isIncrement() ->
                    UNMATCHED

                e1 is JetCallableReferenceExpression && e2 is JetCallableReferenceExpression ->
                    if (matchCallableReferences(e1, e2)) MATCHED else UNMATCHED

                else ->
                    matchCalls(e1, e2)
            }
        }

        private fun PsiElement.checkType(parameter: UnifierParameter): Boolean {
            val targetElementType = (this as? JetExpression)?.let { it.bindingContext[BindingContext.EXPRESSION_TYPE, it] }
            return targetElementType != null && JetTypeChecker.DEFAULT.isSubtypeOf(targetElementType, parameter.expectedType)
        }

        fun doUnify(target: JetPsiRange, pattern: JetPsiRange): Status {
            val targetElements = target.elements
            val patternElements = pattern.elements
            if (targetElements.size != patternElements.size) return UNMATCHED

            return (targetElements.stream() zip patternElements.stream()).fold(MATCHED) {(s, p) ->
                if (s != UNMATCHED) s and doUnify(p.first, p.second) else s
            }
        }

        private fun ASTNode.getChildrenRange(): JetPsiRange =
                getChildren(null).map { it.getPsi() }.filterNotNull().toRange()

        private fun PsiElement.unwrapWeakly(): JetElement? {
            return when {
                this is JetReturnExpression -> getReturnedExpression()
                this is JetProperty -> getInitializer()
                JetPsiUtil.isOrdinaryAssignment(this) -> (this as JetBinaryExpression).getRight()
                this is JetExpression && this !is JetDeclaration -> this
                else -> null
            }
        }

        private fun doUnifyWeakly(
                targetElement: JetElement,
                patternElement: JetElement
        ): Status {
            if (!allowWeakMatches) return UNMATCHED

            val targetElementUnwrapped = targetElement.unwrapWeakly()
            val patternElementUnwrapped = patternElement.unwrapWeakly()
            if (targetElementUnwrapped == null || patternElementUnwrapped == null) return UNMATCHED
            if (targetElementUnwrapped == targetElement && patternElementUnwrapped == patternElement) return UNMATCHED

            val status = doUnify(targetElementUnwrapped, patternElementUnwrapped)
            if (status == MATCHED && allowWeakMatches) {
                weakMatches[patternElement] = targetElement
            }

            return status
        }

        fun doUnify(
                targetElement: PsiElement?,
                patternElement: PsiElement?
        ): Status {
            val targetElementUnwrapped = targetElement?.unwrap()
            val patternElementUnwrapped = patternElement?.unwrap()

            if (targetElementUnwrapped == patternElementUnwrapped) return MATCHED
            if (targetElementUnwrapped == null || patternElementUnwrapped == null) return UNMATCHED

            if (!checkEquivalence) {
                val referencedPatternDescriptor = (patternElementUnwrapped as? JetReferenceExpression)?.let {
                    it.bindingContext[BindingContext.REFERENCE_TARGET, it]
                }
                val parameter = descriptorToParameter[referencedPatternDescriptor]
                if (parameter != null) {
                    if (targetElementUnwrapped !is JetExpression) return UNMATCHED
                    if (!targetElementUnwrapped.checkType(parameter)) return UNMATCHED

                    val existingArgument = substitution[parameter]
                    return when {
                        existingArgument == null -> {
                            substitution[parameter] = targetElementUnwrapped
                            MATCHED
                        }
                        else -> {
                            checkEquivalence = true
                            val status = doUnify(existingArgument, targetElementUnwrapped)
                            checkEquivalence = false

                            status
                        }
                    }
                }
            }

            val targetNode = targetElementUnwrapped.getNode()
            val patternNode = patternElementUnwrapped.getNode()
            if (targetNode == null || patternNode == null) return UNMATCHED

            val resolvedStatus = matchResolvedInfo(targetElementUnwrapped, patternElementUnwrapped)
            if (resolvedStatus == MATCHED) return resolvedStatus

            if (targetElementUnwrapped is JetElement && patternElementUnwrapped is JetElement) {
                val weakStatus = doUnifyWeakly(targetElementUnwrapped, patternElementUnwrapped)
                if (weakStatus != UNMATCHED) return weakStatus
            }

            if (targetNode.getElementType() != patternNode.getElementType()) return UNMATCHED

            if (resolvedStatus != null) return resolvedStatus

            val targetChildren = targetNode.getChildrenRange()
            val patternChildren = patternNode.getChildrenRange()

            if (patternChildren.empty && targetChildren.empty) {
                return if (targetElementUnwrapped.unquotedText() == patternElementUnwrapped.unquotedText()) MATCHED else UNMATCHED
            }

            return doUnify(targetChildren, patternChildren)
        }
    }

    private val descriptorToParameter = ContainerUtil.newMapFromValues(parameters.iterator()) { it!!.descriptor }

    private fun PsiElement.unwrap(): PsiElement? {
        return when (this) {
            is JetExpression -> JetPsiUtil.deparenthesize(this)
            is JetStringTemplateEntryWithExpression -> JetPsiUtil.deparenthesize(getExpression())
            else -> this
        }
    }

    private fun PsiElement.unquotedText(): String {
        val text = getText() ?: ""
        return if (this is LeafPsiElement) JetPsiUtil.unquoteIdentifier(text) else text
    }

    public fun unify(target: JetPsiRange, pattern: JetPsiRange): UnificationResult {
        return with(Context(target, pattern)) {
            val status = doUnify(target, pattern)
            when {
                substitution.size != descriptorToParameter.size ->
                    Unmatched
                status == MATCHED ->
                    if (weakMatches.isEmpty()) StronglyMatched(substitution) else WeaklyMatched(substitution, weakMatches)
                else ->
                    Unmatched
            }
        }
    }

    public fun unify(targetElement: PsiElement?, patternElement: PsiElement?): UnificationResult =
            unify(targetElement.toRange(), patternElement.toRange())
}

public fun PsiElement?.matches(e: PsiElement?): Boolean = JetPsiUnifier.DEFAULT.unify(this, e).matched
public fun JetPsiRange.matches(r: JetPsiRange): Boolean = JetPsiUnifier.DEFAULT.unify(this, r).matched
