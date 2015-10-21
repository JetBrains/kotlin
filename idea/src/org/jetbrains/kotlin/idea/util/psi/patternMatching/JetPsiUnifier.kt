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
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import java.util.Collections
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.psi.KtLabelReferenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.ErrorUtils
import com.intellij.lang.ASTNode
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiRange.Empty
import org.jetbrains.kotlin.psi.KtMultiDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.psi.KtWithExpressionInitializer
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtDelegatorToSuperClass
import org.jetbrains.kotlin.psi.KtDelegationSpecifier
import org.jetbrains.kotlin.idea.core.refactoring.getContextForContainingDeclarationBody
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

public interface UnificationResult {
    public enum class Status {
        MATCHED {
            override fun and(other: Status): Status = other
        },

        UNMATCHED {
            override fun and(other: Status): Status = this
        };

        public abstract fun and(other: Status): Status
    }

    object Unmatched : UnificationResult {
        override val status: Status get() = UNMATCHED
    }

    interface Matched: UnificationResult {
        val substitution: Map<UnifierParameter, KtExpression>
        override val status: Status get() = MATCHED
    }

    class StronglyMatched(override val substitution: Map<UnifierParameter, KtExpression>): Matched

    class WeaklyMatched(
            override val substitution: Map<UnifierParameter, KtExpression>,
            val weakMatches: Map<KtElement, KtElement>
    ): Matched

    val status: Status
    val matched: Boolean get() = status != UNMATCHED
}

public class UnifierParameter(
        val descriptor: DeclarationDescriptor,
        val expectedType: KotlinType
)

public class JetPsiUnifier(
        parameters: Collection<UnifierParameter> = Collections.emptySet(),
        val allowWeakMatches: Boolean = false
) {
    companion object {
        val DEFAULT = JetPsiUnifier()
    }

    private inner class Context(
            val originalTarget: JetPsiRange,
            val originalPattern: JetPsiRange
    ) {
        val patternContext: BindingContext = originalPattern.getBindingContext()
        val targetContext: BindingContext = originalTarget.getBindingContext()
        val substitution = HashMap<UnifierParameter, KtExpression>()
        val declarationPatternsToTargets = MultiMap<DeclarationDescriptor, DeclarationDescriptor>()
        val weakMatches = HashMap<KtElement, KtElement>()
        var checkEquivalence: Boolean = false

        private fun JetPsiRange.getBindingContext(): BindingContext {
            val element = (this as? JetPsiRange.ListRange)?.startElement as? KtElement
            if ((element?.getContainingFile() as? KtFile)?.doNotAnalyze != null) return BindingContext.EMPTY
            return element?.getContextForContainingDeclarationBody() ?: BindingContext.EMPTY
        }

        private fun matchDescriptors(d1: DeclarationDescriptor?, d2: DeclarationDescriptor?): Boolean {
            if (d1 == d2 || d2 in declarationPatternsToTargets[d1] || d1 in declarationPatternsToTargets[d2]) return true
            if (d1 == null || d2 == null) return false

            val decl1 = DescriptorToSourceUtils.descriptorToDeclaration(d1) as? KtDeclaration
            val decl2 = DescriptorToSourceUtils.descriptorToDeclaration(d2) as? KtDeclaration
            if (decl1 == null || decl2 == null) return false
            if (decl1 == decl2) return true

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
                val op1 = (rc1.getCall().getCalleeExpression() as? KtSimpleNameExpression)?.getReferencedNameElementType()
                val op2 = (rc2.getCall().getCalleeExpression() as? KtSimpleNameExpression)?.getReferencedNameElementType()

                return when {
                    op1 == op2 ->
                        true
                    op1 == KtTokens.NOT_IN, op2 == KtTokens.NOT_IN ->
                        false
                    op1 == KtTokens.EXCLEQ, op2 == KtTokens.EXCLEQ ->
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
                if (args1.size() != args2.size()) return UNMATCHED
                if (rc1.getCall().getValueArguments().size() != args1.size() || rc2.getCall().getValueArguments().size() != args2.size()) return null

                return (args1.asSequence() zip args2.asSequence()).fold(MATCHED) { s, p ->
                    val (arg1, arg2) = p
                    s and when {
                        arg1 == arg2 -> MATCHED
                        arg1 == null || arg2 == null -> UNMATCHED
                        else -> (arg1.getArguments().asSequence() zip arg2.getArguments().asSequence()).fold(MATCHED) { s, p ->
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

                val thisExpression = explicitReceiver?.getExpression() as? KtThisExpression
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

        private val KtElement.bindingContext: BindingContext get() = if (this in originalPattern) patternContext else targetContext

        private fun KtElement.getAdjustedResolvedCall(): ResolvedCall<*>? {
            val rc = if (this is KtArrayAccessExpression) {
                bindingContext[BindingContext.INDEXED_LVALUE_GET, this]
            }
            else {
                getResolvedCall(bindingContext)?.let {
                    when {
                        it !is VariableAsFunctionResolvedCall -> it
                        this is KtSimpleNameExpression -> it.variableCall
                        else -> it.functionCall
                    }
                }
            }

            return when {
                rc == null || ErrorUtils.isError(rc.getCandidateDescriptor()) -> null
                else -> rc
            }
        }

        private fun matchCalls(e1: KtElement, e2: KtElement): Status? {
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

        private fun KtTypeReference.getType(): KotlinType? {
            val t = bindingContext[BindingContext.TYPE, this]
            return if (t == null || t.isError()) null else t
        }

        private fun matchTypes(type1: KotlinType?, type2: KotlinType?): Status? {
            if (type1 != null && type2 != null) {
                if (type1.isError() || type2.isError()) return null
                if (TypeUtils.equalTypes(type1, type2)) return MATCHED

                if (type1.isMarkedNullable() != type2.isMarkedNullable()) return UNMATCHED
                if (!matchDescriptors(
                        type1.getConstructor().getDeclarationDescriptor(),
                        type2.getConstructor().getDeclarationDescriptor())) return UNMATCHED

                val args1 = type1.getArguments()
                val args2 = type2.getArguments()
                if (args1.size() != args2.size()) return UNMATCHED
                if (!args1.zip(args2).all {
                    it.first.getProjectionKind() == it.second.getProjectionKind() && matchTypes(it.first.getType(), it.second.getType()) == MATCHED }
                ) return UNMATCHED

                return MATCHED
            }

            return if (type1 == null && type2 == null) null else UNMATCHED
        }

        private fun matchTypes(types1: Collection<KotlinType>, types2: Collection<KotlinType>): Boolean {
            fun sortTypes(types: Collection<KotlinType>) = types.sortedBy { DescriptorRenderer.DEBUG_TEXT.renderType(it) }

            if (types1.size() != types2.size()) return false
            return (sortTypes(types1) zip sortTypes(types2)).all { matchTypes(it.first, it.second) == MATCHED }
        }

        private fun KtElement.shouldIgnoreResolvedCall(): Boolean {
            return when {
                this is KtConstantExpression -> true
                this is KtOperationReferenceExpression -> getReferencedNameElementType() == KtTokens.EXCLEXCL
                this is KtIfExpression -> true
                this is KtUnaryExpression -> when (getOperationReference().getReferencedNameElementType()) {
                    KtTokens.EXCLEXCL, KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> true
                    else -> false
                }
                this is KtBinaryExpression -> getOperationReference().getReferencedNameElementType() == KtTokens.ELVIS
                else -> false
            }
        }

        private fun KtBinaryExpression.matchComplexAssignmentWithSimple(simple: KtBinaryExpression): Status? {
            return when {
                doUnify(getLeft(), simple.getLeft()) == UNMATCHED -> UNMATCHED
                else -> simple.getRight()?.let { matchCalls(this, it) } ?: UNMATCHED
            }
        }

        private fun KtBinaryExpression.matchAssignment(e: KtElement): Status? {
            val operationType = getOperationReference().getReferencedNameElementType() as KtToken
            if (operationType == KtTokens.EQ) {
                if (e.shouldIgnoreResolvedCall()) return UNMATCHED

                if (KtPsiUtil.isAssignment(e) && !KtPsiUtil.isOrdinaryAssignment(e)) {
                    return (e as KtBinaryExpression).matchComplexAssignmentWithSimple(this)
                }

                val lhs = getLeft()?.unwrap()
                if (lhs !is KtArrayAccessExpression) return null

                val setResolvedCall = bindingContext[BindingContext.INDEXED_LVALUE_SET, lhs]
                val resolvedCallToMatch = e.getAdjustedResolvedCall()

                return if (setResolvedCall == null || resolvedCallToMatch == null) null else matchResolvedCalls(setResolvedCall, resolvedCallToMatch)
            }

            val assignResolvedCall = getAdjustedResolvedCall()
            if (assignResolvedCall == null) return UNMATCHED

            val operationName = OperatorConventions.getNameForOperationSymbol(operationType)
            if (assignResolvedCall.getResultingDescriptor()?.getName() == operationName) return matchCalls(this, e)

            return if (KtPsiUtil.isAssignment(e)) null else UNMATCHED
        }

        private fun matchLabelTargets(e1: KtLabelReferenceExpression, e2: KtLabelReferenceExpression): Status {
            val target1 = e1.bindingContext[BindingContext.LABEL_TARGET, e1]
            val target2 = e2.bindingContext[BindingContext.LABEL_TARGET, e2]

            return if (target1 == target2) MATCHED else UNMATCHED
        }

        private fun PsiElement.isIncrement(): Boolean {
            val parent = getParent()
            return parent is KtUnaryExpression
                   && this == parent.getOperationReference()
                   && ((parent.getOperationToken() as KtToken) in OperatorConventions.INCREMENT_OPERATIONS)
        }

        private fun matchCallableReferences(e1: KtCallableReferenceExpression, e2: KtCallableReferenceExpression): Boolean {
            val d1 = e1.bindingContext[BindingContext.REFERENCE_TARGET, e1.getCallableReference()]
            val d2 = e2.bindingContext[BindingContext.REFERENCE_TARGET, e2.getCallableReference()]
            return matchDescriptors(d1, d2)
        }

        private fun matchMultiDeclarations(e1: KtMultiDeclaration, e2: KtMultiDeclaration): Boolean {
            val entries1 = e1.getEntries()
            val entries2 = e2.getEntries()
            if (entries1.size() != entries2.size()) return false

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
                decl1: KtDeclaration,
                decl2: KtDeclaration,
                desc1: CallableDescriptor,
                desc2: CallableDescriptor): Status? {
            fun needToCompareReturnTypes(): Boolean {
                if (decl1 !is KtCallableDeclaration) return true
                return decl1.getTypeReference() != null || (decl2 as KtCallableDeclaration).getTypeReference() != null
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
                    (params1.size() == params2.size()) && zippedParams.all { matchTypes(it.first.getType(), it.second.getType()) == MATCHED }
            if (!parametersMatch) return UNMATCHED

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }

            return doUnify(
                    (decl1 as? KtTypeParameterListOwner)?.getTypeParameters()?.toRange() ?: Empty,
                    (decl2 as? KtTypeParameterListOwner)?.getTypeParameters()?.toRange() ?: Empty
            ) and when (decl1) {
                is KtDeclarationWithBody ->
                    doUnify(decl1.getBodyExpression(), (decl2 as KtDeclarationWithBody).getBodyExpression())

                is KtWithExpressionInitializer ->
                    doUnify(decl1.getInitializer(), (decl2 as KtWithExpressionInitializer).getInitializer())

                is KtParameter ->
                    doUnify(decl1.getDefaultValue(), (decl2 as KtParameter).getDefaultValue())

                else ->
                    UNMATCHED
            }
        }

        private fun KtDeclaration.isNameRelevant(): Boolean {
            if (this is KtParameter && hasValOrVar()) return true

            val parent = getParent()
            return parent is KtClassBody || parent is KtFile
        }

        private fun matchNames(decl1: KtDeclaration, decl2: KtDeclaration, desc1: DeclarationDescriptor, desc2: DeclarationDescriptor): Boolean {
            return (!decl1.isNameRelevant() && !decl2.isNameRelevant()) || desc1.getName() == desc2.getName()
        }

        private fun matchContainedDescriptors<T: DeclarationDescriptor>(
                declarations1: List<T>,
                declarations2: List<T>,
                matchPair: (Pair<T, T>) -> Boolean
        ): Boolean {
            val zippedParams = declarations1 zip declarations2
            if (declarations1.size() != declarations2.size() || !zippedParams.all { matchPair(it) }) return false

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }
            return true
        }

        private fun matchClasses(
                decl1: KtClassOrObject,
                decl2: KtClassOrObject,
                desc1: ClassDescriptor,
                desc2: ClassDescriptor): Status? {
            class OrderInfo<T>(
                    val orderSensitive: List<T>,
                    val orderInsensitive: List<T>
            )

            fun getMemberOrderInfo(cls: KtClassOrObject): OrderInfo<KtDeclaration> {
                val (orderInsensitive, orderSensitive) = (cls.getBody()?.getDeclarations() ?: Collections.emptyList()).partition {
                    it is KtClassOrObject || it is KtFunction
                }

                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun getDelegationOrderInfo(cls: KtClassOrObject): OrderInfo<KtDelegationSpecifier> {
                val (orderInsensitive, orderSensitive) = cls.getDelegationSpecifiers().partition { it is KtDelegatorToSuperClass }
                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun resolveAndSortDeclarationsByDescriptor(declarations: List<KtDeclaration>): List<Pair<KtDeclaration, DeclarationDescriptor?>> {
                return declarations
                        .map { it to it.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] }
                        .sortedBy { it.second?.let { IdeDescriptorRenderers.SOURCE_CODE.render(it) } ?: "" }
            }

            fun sortDeclarationsByElementType(declarations: List<KtDeclaration>): List<KtDeclaration> {
                return declarations.sortedBy { it.getNode()?.getElementType()?.getIndex() ?: -1 }
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

            if (delegationInfo1.orderInsensitive.size() != delegationInfo2.orderInsensitive.size()) return UNMATCHED
            outer@
            for (specifier1 in delegationInfo1.orderInsensitive) {
                for (specifier2 in delegationInfo2.orderInsensitive) {
                    if (doUnify(specifier1, specifier2) != UNMATCHED) continue@outer
                }
                return UNMATCHED
            }

            val status = doUnify((decl1 as? KtClass)?.getPrimaryConstructorParameterList(), (decl2 as? KtClass)?.getPrimaryConstructorParameterList()) and
                    doUnify((decl1 as? KtClass)?.getTypeParameterList(), (decl2 as? KtClass)?.getTypeParameterList()) and
                    doUnify(delegationInfo1.orderSensitive.toRange(), delegationInfo2.orderSensitive.toRange())
            if (status == UNMATCHED) return UNMATCHED

            val membersInfo1 = getMemberOrderInfo(decl1)
            val membersInfo2 = getMemberOrderInfo(decl2)

            val sortedMembers1 = resolveAndSortDeclarationsByDescriptor(membersInfo1.orderInsensitive)
            val sortedMembers2 = resolveAndSortDeclarationsByDescriptor(membersInfo2.orderInsensitive)
            if ((sortedMembers1.size() != sortedMembers2.size())) return UNMATCHED
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

        private fun KtDeclaration.matchDeclarations(e: PsiElement): Status? {
            if (e !is KtDeclaration) return UNMATCHED

            val desc1 = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this]
            val desc2 = e.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, e]
            return matchDeclarations(this, e, desc1, desc2)
        }

        private fun matchDeclarations(
                decl1: KtDeclaration,
                decl2: KtDeclaration,
                desc1: DeclarationDescriptor?,
                desc2: DeclarationDescriptor?): Status? {
            if (decl1.javaClass != decl2.javaClass) return UNMATCHED

            if (desc1 == null || desc2 == null || ErrorUtils.isError(desc1) || ErrorUtils.isError(desc2)) return UNMATCHED
            if (desc1.javaClass != desc2.javaClass) return UNMATCHED

            declarationPatternsToTargets.putValue(desc1, desc2)
            val status = when (decl1) {
                is KtDeclarationWithBody, is KtWithExpressionInitializer, is KtParameter ->
                    matchCallables(decl1, decl2, desc1 as CallableDescriptor, desc2 as CallableDescriptor)

                is KtClassOrObject ->
                    matchClasses(decl1, decl2 as KtClassOrObject, desc1 as ClassDescriptor, desc2 as ClassDescriptor)

                is KtTypeParameter ->
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
                e1 !is KtElement, e2 !is KtElement ->
                    null

                e1 is KtMultiDeclaration && e2 is KtMultiDeclaration ->
                    if (matchMultiDeclarations(e1, e2)) null else UNMATCHED

                e1 is KtClassInitializer && e2 is KtClassInitializer ->
                    null

                e1 is KtDeclaration ->
                    e1.matchDeclarations(e2)

                e2 is KtDeclaration ->
                    e2.matchDeclarations(e1)

                e1 is KtTypeReference && e2 is KtTypeReference ->
                    matchTypes(e1.getType(), e2.getType())

                KtPsiUtil.isAssignment(e1) ->
                    (e1 as KtBinaryExpression).matchAssignment(e2)

                KtPsiUtil.isAssignment(e2) ->
                    (e2 as KtBinaryExpression).matchAssignment(e1)

                e1 is KtLabelReferenceExpression && e2 is KtLabelReferenceExpression ->
                    matchLabelTargets(e1, e2)

                e1.isIncrement() != e2.isIncrement() ->
                    UNMATCHED

                e1 is KtCallableReferenceExpression && e2 is KtCallableReferenceExpression ->
                    if (matchCallableReferences(e1, e2)) MATCHED else UNMATCHED

                else ->
                    matchCalls(e1, e2)
            }
        }

        private fun PsiElement.checkType(parameter: UnifierParameter): Boolean {
            val targetElementType = (this as? KtExpression)?.let { it.bindingContext.getType(it) }
            return targetElementType != null && KotlinTypeChecker.DEFAULT.isSubtypeOf(targetElementType, parameter.expectedType)
        }

        fun doUnify(target: JetPsiRange, pattern: JetPsiRange): Status {
            val targetElements = target.elements
            val patternElements = pattern.elements
            if (targetElements.size() != patternElements.size()) return UNMATCHED

            return (targetElements.asSequence() zip patternElements.asSequence()).fold(MATCHED) { s, p ->
                if (s != UNMATCHED) s and doUnify(p.first, p.second) else s
            }
        }

        private fun ASTNode.getChildrenRange(): JetPsiRange =
                getChildren(null).map { it.getPsi() }.filterNotNull().toRange()

        private fun PsiElement.unwrapWeakly(): KtElement? {
            return when {
                this is KtReturnExpression -> getReturnedExpression()
                this is KtProperty -> getInitializer()
                KtPsiUtil.isOrdinaryAssignment(this) -> (this as KtBinaryExpression).getRight()
                this is KtExpression && this !is KtDeclaration -> this
                else -> null
            }
        }

        private fun doUnifyWeakly(
                targetElement: KtElement,
                patternElement: KtElement
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
                val referencedPatternDescriptor = (patternElementUnwrapped as? KtReferenceExpression)?.let {
                    it.bindingContext[BindingContext.REFERENCE_TARGET, it]
                }
                val parameter = descriptorToParameter[referencedPatternDescriptor]
                if (parameter != null) {
                    if (targetElementUnwrapped !is KtExpression) return UNMATCHED
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

            if (targetElementUnwrapped is KtElement && patternElementUnwrapped is KtElement) {
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
            is KtExpression -> KtPsiUtil.deparenthesize(this)
            is KtStringTemplateEntryWithExpression -> KtPsiUtil.deparenthesize(getExpression())
            else -> this
        }
    }

    private fun PsiElement.unquotedText(): String {
        val text = getText() ?: ""
        return if (this is LeafPsiElement) KtPsiUtil.unquoteIdentifier(text) else text
    }

    public fun unify(target: JetPsiRange, pattern: JetPsiRange): UnificationResult {
        return with(Context(target, pattern)) {
            val status = doUnify(target, pattern)
            when {
                substitution.size() != descriptorToParameter.size() ->
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
