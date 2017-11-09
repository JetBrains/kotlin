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

package org.jetbrains.kotlin.idea.util.psi.patternMatching

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.introduce.ExtractableSubstringInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractableSubstringInfo
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange.Empty
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.*
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.Status.MATCHED
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnificationResult.Status.UNMATCHED
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import java.util.*

interface UnificationResult {
    enum class Status {
        MATCHED {
            override fun and(other: Status): Status = other
        },

        UNMATCHED {
            override fun and(other: Status): Status = this
        };

        abstract infix fun and(other: Status): Status
    }

    object Unmatched : UnificationResult {
        override val status: Status get() = UNMATCHED
    }

    interface Matched: UnificationResult {
        val range: KotlinPsiRange
        val substitution: Map<UnifierParameter, KtElement>
        override val status: Status get() = MATCHED
    }

    class StronglyMatched(
            override val range: KotlinPsiRange,
            override val substitution: Map<UnifierParameter, KtElement>
    ): Matched

    class WeaklyMatched(
            override val range: KotlinPsiRange,
            override val substitution: Map<UnifierParameter, KtElement>,
            val weakMatches: Map<KtElement, KtElement>
    ): Matched

    val status: Status
    val matched: Boolean get() = status != UNMATCHED
}

class UnifierParameter(
        val descriptor: DeclarationDescriptor,
        val expectedType: KotlinType?
)

class KotlinPsiUnifier(
        parameters: Collection<UnifierParameter> = Collections.emptySet(),
        val allowWeakMatches: Boolean = false
) {
    companion object {
        val DEFAULT = KotlinPsiUnifier()
    }

    private inner class Context(
            val originalTarget: KotlinPsiRange,
            val originalPattern: KotlinPsiRange
    ) {
        val patternContext: BindingContext = originalPattern.getBindingContext()
        val targetContext: BindingContext = originalTarget.getBindingContext()
        val substitution = HashMap<UnifierParameter, KtElement>()
        val declarationPatternsToTargets = MultiMap<DeclarationDescriptor, DeclarationDescriptor>()
        val weakMatches = HashMap<KtElement, KtElement>()
        var checkEquivalence: Boolean = false
        var targetSubstringInfo: ExtractableSubstringInfo? = null

        private fun KotlinPsiRange.getBindingContext(): BindingContext {
            val element = (this as? KotlinPsiRange.ListRange)?.startElement as? KtElement
            if ((element?.containingFile as? KtFile)?.doNotAnalyze != null) return BindingContext.EMPTY
            return element?.analyze() ?: BindingContext.EMPTY
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

        private fun matchReceivers(rv1: Receiver?, rv2: Receiver?): Boolean {
            return when {
                rv1 is ExpressionReceiver && rv2 is ExpressionReceiver ->
                    doUnify(rv1.expression, rv2.expression) == MATCHED

                rv1 is ImplicitReceiver && rv2 is ImplicitReceiver ->
                    matchDescriptors(rv1.declarationDescriptor, rv2.declarationDescriptor)

                else ->
                    rv1 == rv2
            }
        }

        private fun matchCalls(call1: Call, call2: Call): Boolean {
            return matchReceivers(call1.explicitReceiver, call2.explicitReceiver) &&
                   matchReceivers(call1.dispatchReceiver, call2.dispatchReceiver)
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
                val op1 = (rc1.call.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameElementType()
                val op2 = (rc2.call.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameElementType()

                return when {
                    op1 == op2 ->
                        true
                    op1 == KtTokens.NOT_IN || op2 == KtTokens.NOT_IN ->
                        false
                    op1 == KtTokens.EXCLEQ || op2 == KtTokens.EXCLEQ ->
                        false
                    op1 in OperatorConventions.COMPARISON_OPERATIONS || op2 in OperatorConventions.COMPARISON_OPERATIONS ->
                        false
                    else ->
                        true
                }
            }

            fun checkArguments(): Status? {
                val args1 = rc1.resultingDescriptor?.valueParameters?.map { rc1.valueArguments[it] } ?: Collections.emptyList()
                val args2 = rc2.resultingDescriptor?.valueParameters?.map { rc2.valueArguments[it] } ?: Collections.emptyList()
                if (args1.size != args2.size) return UNMATCHED
                if (rc1.call.valueArguments.size != args1.size || rc2.call.valueArguments.size != args2.size) return null

                return (args1.asSequence().zip(args2.asSequence())).fold(MATCHED) { s, p ->
                    val (arg1, arg2) = p
                    s and when {
                        arg1 == arg2 -> MATCHED
                        arg1 == null || arg2 == null -> UNMATCHED
                        else -> (arg1.arguments.asSequence().zip(arg2.arguments.asSequence())).fold(MATCHED) { s, p ->
                            s and matchArguments(p.first, p.second)
                        }
                    }
                }
            }

            fun checkImplicitReceiver(implicitCall: ResolvedCall<*>, explicitCall: ResolvedCall<*>): Boolean {
                val (implicitReceiver, explicitReceiver) =
                        when (explicitCall.explicitReceiverKind) {
                            ExplicitReceiverKind.EXTENSION_RECEIVER ->
                                (implicitCall.extensionReceiver as? ImplicitReceiver) to
                                        (explicitCall.extensionReceiver as? ExpressionReceiver)

                            ExplicitReceiverKind.DISPATCH_RECEIVER ->
                                (implicitCall.dispatchReceiver as? ImplicitReceiver) to
                                        (explicitCall.dispatchReceiver as? ExpressionReceiver)

                            else ->
                                null to null
                        }

                val thisExpression = explicitReceiver?.expression as? KtThisExpression
                if (implicitReceiver == null || thisExpression == null) return false

                return matchDescriptors(
                        implicitReceiver.declarationDescriptor,
                        thisExpression.getAdjustedResolvedCall()?.candidateDescriptor?.containingDeclaration
                )
            }

            fun checkReceivers(): Boolean {
                return when {
                    rc1.explicitReceiverKind == rc2.explicitReceiverKind -> {
                        matchReceivers(rc1.extensionReceiver, rc2.extensionReceiver) &&
                        (rc1.explicitReceiverKind == ExplicitReceiverKind.BOTH_RECEIVERS || matchReceivers(rc1.dispatchReceiver, rc2.dispatchReceiver))
                    }

                    rc1.explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> checkImplicitReceiver(rc1, rc2)

                    rc2.explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER -> checkImplicitReceiver(rc2, rc1)

                    else -> false
                }
            }

            fun checkTypeArguments(): Status? {
                val typeArgs1 = rc1.typeArguments.toList()
                val typeArgs2 = rc2.typeArguments.toList()
                if (typeArgs1.size != typeArgs2.size) return UNMATCHED

                for ((typeArg1, typeArg2) in (typeArgs1.zip(typeArgs2))) {
                    if (!matchDescriptors(typeArg1.first, typeArg2.first)) return UNMATCHED

                    val s = matchTypes(typeArg1.second, typeArg2.second)
                    if (s != MATCHED) return s
                }

                return MATCHED
            }

            return when {
                !checkSpecialOperations() -> UNMATCHED
                !matchDescriptors(rc1.candidateDescriptor, rc2.candidateDescriptor) -> UNMATCHED
                !checkReceivers() -> UNMATCHED
                rc1.call.isSafeCall() != rc2.call.isSafeCall() -> UNMATCHED
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
                rc == null || ErrorUtils.isError(rc.candidateDescriptor) -> null
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
            val t = bindingContext.let { it[BindingContext.ABBREVIATED_TYPE, this] ?: it[BindingContext.TYPE, this] }
            return if (t == null || t.isError) null else t
        }

        private fun matchTypes(
                type1: KotlinType?,
                type2: KotlinType?,
                typeRef1: KtTypeReference? = null,
                typeRef2: KtTypeReference? = null
        ): Status? {
            if (type1 != null && type2 != null) {
                val unwrappedType1 = type1.unwrap()
                val unwrappedType2 = type2.unwrap()
                if (unwrappedType1 !== type1 || unwrappedType2 !== type2) return matchTypes(unwrappedType1, unwrappedType2, typeRef1, typeRef2)

                if (type1.isError || type2.isError) return null
                if (type1 is AbbreviatedType != type2 is AbbreviatedType) return UNMATCHED
                if (type1.isExtensionFunctionType != type2.isExtensionFunctionType) return UNMATCHED
                if (TypeUtils.equalTypes(type1, type2)) return MATCHED

                if (type1.isMarkedNullable != type2.isMarkedNullable) return UNMATCHED
                if (!matchDescriptors(
                        type1.constructor.declarationDescriptor,
                        type2.constructor.declarationDescriptor)) return UNMATCHED

                val args1 = type1.arguments
                val args2 = type2.arguments
                if (args1.size != args2.size) return UNMATCHED
                if (!args1.withIndex().all { p ->
                    val (i, arg1) = p
                    val arg2 = args2[i]
                    matchTypeArguments(i, arg1, arg2, typeRef1, typeRef2)
                }) return UNMATCHED

                return MATCHED
            }

            return if (type1 == null && type2 == null) null else UNMATCHED
        }

        private fun matchTypeArguments(
                argIndex: Int,
                arg1: TypeProjection,
                arg2: TypeProjection,
                typeRef1: KtTypeReference?,
                typeRef2: KtTypeReference?
        ): Boolean {
            val typeArgRef1 = typeRef1?.typeElement?.typeArgumentsAsTypes?.getOrNull(argIndex)
            val typeArgRef2 = typeRef2?.typeElement?.typeArgumentsAsTypes?.getOrNull(argIndex)

            if (arg1.projectionKind != arg2.projectionKind) return false
            val argType1 = arg1.type
            val argType2 = arg2.type
            // Substitution attempt using either arg1, or arg2 as a pattern type. Falls back to exact matching if substitution is not possible
            val status = if (!checkEquivalence && typeRef1 != null && typeRef2 != null) {
                val typePsi1 = argType1.constructor.declarationDescriptor?.source?.getPsi()
                val typePsi2 = argType2.constructor.declarationDescriptor?.source?.getPsi()
                descriptorToParameter[typePsi1]?.let { substitute(it, typeArgRef2) } ?:
                descriptorToParameter[typePsi2]?.let { substitute(it, typeArgRef1) } ?:
                matchTypes(argType1, argType2, typeArgRef1, typeArgRef2)
            }
            else matchTypes(argType1, argType2, typeArgRef1, typeArgRef2)

            return status == MATCHED
        }

        private fun matchTypes(types1: Collection<KotlinType>, types2: Collection<KotlinType>): Boolean {
            fun sortTypes(types: Collection<KotlinType>) = types.sortedBy { DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it) }

            if (types1.size != types2.size) return false
            return (sortTypes(types1).zip(sortTypes(types2))).all { matchTypes(it.first, it.second) == MATCHED }
        }

        private fun KtElement.shouldIgnoreResolvedCall() = when (this) {
            is KtConstantExpression -> true
            is KtOperationReferenceExpression -> getReferencedNameElementType() == KtTokens.EXCLEXCL
            is KtIfExpression -> true
            is KtWhenExpression -> true
            is KtUnaryExpression -> when (operationReference.getReferencedNameElementType()) {
                KtTokens.EXCLEXCL, KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> true
                else -> false
            }
            is KtBinaryExpression -> operationReference.getReferencedNameElementType() == KtTokens.ELVIS
            is KtThisExpression -> true
            is KtSimpleNameExpression -> getStrictParentOfType<KtTypeElement>() != null
            else -> false
        }

        private fun KtBinaryExpression.matchComplexAssignmentWithSimple(simple: KtBinaryExpression): Status? {
            return when {
                doUnify(left, simple.left) == UNMATCHED -> UNMATCHED
                else -> simple.right?.let { matchCalls(this, it) } ?: UNMATCHED
            }
        }

        private fun KtBinaryExpression.matchAssignment(e: KtElement): Status? {
            val operationType = operationReference.getReferencedNameElementType() as KtToken
            if (operationType == KtTokens.EQ) {
                if (e.shouldIgnoreResolvedCall()) return UNMATCHED

                if (KtPsiUtil.isAssignment(e) && !KtPsiUtil.isOrdinaryAssignment(e)) {
                    return (e as KtBinaryExpression).matchComplexAssignmentWithSimple(this)
                }

                val lhs = left?.unwrap()
                if (lhs !is KtArrayAccessExpression) return null

                val setResolvedCall = bindingContext[BindingContext.INDEXED_LVALUE_SET, lhs]
                val resolvedCallToMatch = e.getAdjustedResolvedCall()

                return if (setResolvedCall == null || resolvedCallToMatch == null) null else matchResolvedCalls(setResolvedCall, resolvedCallToMatch)
            }

            val assignResolvedCall = getAdjustedResolvedCall()
            if (assignResolvedCall == null) return UNMATCHED

            val operationName = OperatorConventions.getNameForOperationSymbol(operationType)
            if (assignResolvedCall.resultingDescriptor?.name == operationName) return matchCalls(this, e)

            return if (KtPsiUtil.isAssignment(e)) null else UNMATCHED
        }

        private fun matchLabelTargets(e1: KtLabelReferenceExpression, e2: KtLabelReferenceExpression): Status {
            val target1 = e1.bindingContext[BindingContext.LABEL_TARGET, e1]
            val target2 = e2.bindingContext[BindingContext.LABEL_TARGET, e2]

            return if (target1 == target2) MATCHED else UNMATCHED
        }

        private fun PsiElement.isIncrement(): Boolean {
            val parent = parent
            return parent is KtUnaryExpression
                   && this == parent.operationReference
                   && ((parent.operationToken as KtToken) in OperatorConventions.INCREMENT_OPERATIONS)
        }

        private fun matchCallableReferences(e1: KtCallableReferenceExpression, e2: KtCallableReferenceExpression): Boolean {
            val d1 = e1.bindingContext[BindingContext.REFERENCE_TARGET, e1.callableReference]
            val d2 = e2.bindingContext[BindingContext.REFERENCE_TARGET, e2.callableReference]
            return matchDescriptors(d1, d2)
        }

        private fun matchThisExpressions(e1: KtThisExpression, e2: KtThisExpression): Boolean {
            val d1 = e1.bindingContext[BindingContext.REFERENCE_TARGET, e1.instanceReference]
            val d2 = e2.bindingContext[BindingContext.REFERENCE_TARGET, e2.instanceReference]
            return matchDescriptors(d1, d2)
        }

        private fun matchDestructuringDeclarations(e1: KtDestructuringDeclaration, e2: KtDestructuringDeclaration): Boolean {
            val entries1 = e1.entries
            val entries2 = e2.entries
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
                receiver1 != null && receiver2 != null -> matchTypes(receiver1.type, receiver2.type) == MATCHED
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
                return decl1.typeReference != null || (decl2 as KtCallableDeclaration).typeReference != null
            }

            if (desc1 is VariableDescriptor && desc1.isVar != (desc2 as VariableDescriptor).isVar) return UNMATCHED

            if (!matchNames(decl1, decl2, desc1, desc2)) return UNMATCHED

            if (needToCompareReturnTypes()) {
                val type1 = desc1.returnType
                val type2 = desc2.returnType

                if (type1 != type2
                    && (type1 == null || type2 == null || type1.isError || type2.isError || matchTypes(type1, type2) != MATCHED)) {
                    return UNMATCHED
                }
            }

            if (!matchReceiverParameters(desc1.extensionReceiverParameter, desc2.extensionReceiverParameter)) return UNMATCHED
            if (!matchReceiverParameters(desc1.dispatchReceiverParameter, desc2.dispatchReceiverParameter)) return UNMATCHED

            val params1 = desc1.valueParameters
            val params2 = desc2.valueParameters
            val zippedParams = params1.zip(params2)
            val parametersMatch =
                    (params1.size == params2.size) && zippedParams.all { matchTypes(it.first.type, it.second.type) == MATCHED }
            if (!parametersMatch) return UNMATCHED

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }

            return doUnify(
                    (decl1 as? KtTypeParameterListOwner)?.typeParameters?.toRange() ?: Empty,
                    (decl2 as? KtTypeParameterListOwner)?.typeParameters?.toRange() ?: Empty
            ) and when (decl1) {
                is KtDeclarationWithBody ->
                    doUnify(decl1.bodyExpression, (decl2 as KtDeclarationWithBody).bodyExpression)

                is KtDeclarationWithInitializer ->
                    doUnify(decl1.initializer, (decl2 as KtDeclarationWithInitializer).initializer)

                is KtParameter ->
                    doUnify(decl1.defaultValue, (decl2 as KtParameter).defaultValue)

                else ->
                    UNMATCHED
            }
        }

        private fun KtDeclaration.isNameRelevant(): Boolean {
            if (this is KtParameter && hasValOrVar()) return true

            val parent = parent
            return parent is KtClassBody || parent is KtFile
        }

        private fun matchNames(decl1: KtDeclaration, decl2: KtDeclaration, desc1: DeclarationDescriptor, desc2: DeclarationDescriptor): Boolean {
            return (!decl1.isNameRelevant() && !decl2.isNameRelevant()) || desc1.name == desc2.name
        }

        private fun <T: DeclarationDescriptor> matchContainedDescriptors(
                declarations1: List<T>,
                declarations2: List<T>,
                matchPair: (Pair<T, T>) -> Boolean
        ): Boolean {
            val zippedParams = declarations1.zip(declarations2)
            if (declarations1.size != declarations2.size || !zippedParams.all { matchPair(it) }) return false

            zippedParams.forEach { declarationPatternsToTargets.putValue(it.first, it.second) }
            return true
        }

        private fun matchClasses(
                decl1: KtClassOrObject,
                decl2: KtClassOrObject,
                desc1: ClassDescriptor,
                desc2: ClassDescriptor): Status? {
            class OrderInfo<out T>(
                    val orderSensitive: List<T>,
                    val orderInsensitive: List<T>
            )

            fun getMemberOrderInfo(cls: KtClassOrObject): OrderInfo<KtDeclaration> {
                val (orderInsensitive, orderSensitive) = (cls.getBody()?.declarations ?: Collections.emptyList()).partition {
                    it is KtClassOrObject || it is KtFunction
                }

                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun getDelegationOrderInfo(cls: KtClassOrObject): OrderInfo<KtSuperTypeListEntry> {
                val (orderInsensitive, orderSensitive) = cls.superTypeListEntries.partition { it is KtSuperTypeEntry }
                return OrderInfo(orderSensitive, orderInsensitive)
            }

            fun resolveAndSortDeclarationsByDescriptor(declarations: List<KtDeclaration>): List<Pair<KtDeclaration, DeclarationDescriptor?>> {
                return declarations
                        .map { it to it.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it] }
                        .sortedBy { it.second?.let { IdeDescriptorRenderers.SOURCE_CODE.render(it) } ?: "" }
            }

            fun sortDeclarationsByElementType(declarations: List<KtDeclaration>): List<KtDeclaration> {
                return declarations.sortedBy { it.node?.elementType?.index ?: -1 }
            }

            if (desc1.kind != desc2.kind) return UNMATCHED
            if (!matchNames(decl1, decl2, desc1, desc2)) return UNMATCHED

            declarationPatternsToTargets.putValue(desc1.thisAsReceiverParameter, desc2.thisAsReceiverParameter)

            val constructor1 = desc1.unsubstitutedPrimaryConstructor
            val constructor2 = desc2.unsubstitutedPrimaryConstructor
            if (constructor1 != null && constructor2 != null) {
                declarationPatternsToTargets.putValue(constructor1, constructor2)
            }

            val delegationInfo1 = getDelegationOrderInfo(decl1)
            val delegationInfo2 = getDelegationOrderInfo(decl2)

            if (delegationInfo1.orderInsensitive.size != delegationInfo2.orderInsensitive.size) return UNMATCHED
            outer@
            for (specifier1 in delegationInfo1.orderInsensitive) {
                for (specifier2 in delegationInfo2.orderInsensitive) {
                    if (doUnify(specifier1, specifier2) != UNMATCHED) continue@outer
                }
                return UNMATCHED
            }

            val status = doUnify((decl1 as? KtClass)?.getPrimaryConstructorParameterList(), (decl2 as? KtClass)?.getPrimaryConstructorParameterList()) and
                    doUnify((decl1 as? KtClass)?.typeParameterList, (decl2 as? KtClass)?.typeParameterList) and
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
            if (desc1.variance != desc2.variance) return UNMATCHED
            if (!matchTypes(desc1.upperBounds, desc2.upperBounds)) return UNMATCHED
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
            if (decl1::class.java != decl2::class.java) return UNMATCHED

            if (desc1 == null || desc2 == null) {
                if (decl1 is KtParameter
                    && decl2 is KtParameter
                    && decl1.getStrictParentOfType<KtTypeElement>() != null
                    && decl2.getStrictParentOfType<KtTypeElement>() != null) return null
                return UNMATCHED
            }
            if (ErrorUtils.isError(desc1) || ErrorUtils.isError(desc2)) return UNMATCHED
            if (desc1::class.java != desc2::class.java) return UNMATCHED

            declarationPatternsToTargets.putValue(desc1, desc2)
            val status = when (decl1) {
                is KtDeclarationWithBody, is KtDeclarationWithInitializer, is KtParameter ->
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
                e1 !is KtElement || e2 !is KtElement ->
                    null

                e1 is KtDestructuringDeclaration && e2 is KtDestructuringDeclaration ->
                    if (matchDestructuringDeclarations(e1, e2)) null else UNMATCHED

                e1 is KtAnonymousInitializer && e2 is KtAnonymousInitializer ->
                    null

                e1 is KtDeclaration ->
                    e1.matchDeclarations(e2)

                e2 is KtDeclaration ->
                    e2.matchDeclarations(e1)

                e1 is KtTypeElement && e2 is KtTypeElement && e1.parent is KtTypeReference && e2.parent is KtTypeReference ->
                    matchResolvedInfo(e1.parent, e2.parent)

                e1 is KtTypeReference && e2 is KtTypeReference ->
                    matchTypes(e1.getType(), e2.getType(), e1, e2)

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

                e1 is KtThisExpression && e2 is KtThisExpression -> if (matchThisExpressions(e1, e2)) MATCHED else UNMATCHED

                else ->
                    matchCalls(e1, e2)
            }
        }

        private fun PsiElement.checkType(parameter: UnifierParameter): Boolean {
            val expectedType = parameter.expectedType ?: return true
            val targetElementType = (this as? KtExpression)?.let { it.bindingContext.getType(it) }
            return targetElementType != null && KotlinTypeChecker.DEFAULT.isSubtypeOf(targetElementType, expectedType)
        }

        private fun doUnifyStringTemplateFragments(target: KtStringTemplateExpression, pattern: ExtractableSubstringInfo): Status {
            val prefixLength = pattern.prefix.length
            val suffixLength = pattern.suffix.length
            val targetEntries = target.entries
            val patternEntries = pattern.entries.toList()
            for ((index, targetEntry) in targetEntries.withIndex()) {
                if (index + patternEntries.size > targetEntries.size) return UNMATCHED

                val targetEntryText = targetEntry.text

                if (pattern.startEntry == pattern.endEntry && (prefixLength > 0 || suffixLength > 0)) {
                    if (targetEntry !is KtLiteralStringTemplateEntry) continue

                    val patternText = with(pattern.startEntry.text) { substring(prefixLength, length - suffixLength) }
                    val i = targetEntryText.indexOf(patternText)
                    if (i < 0) continue
                    val targetPrefix = targetEntryText.substring(0, i)
                    val targetSuffix = targetEntryText.substring(i + patternText.length)
                    targetSubstringInfo = ExtractableSubstringInfo(targetEntry, targetEntry, targetPrefix, targetSuffix, pattern.type)
                    return MATCHED
                }

                val matchStartByText = pattern.startEntry is KtLiteralStringTemplateEntry
                val matchEndByText = pattern.endEntry is KtLiteralStringTemplateEntry

                val targetPrefix = if (matchStartByText) {
                    if (targetEntry !is KtLiteralStringTemplateEntry) continue

                    val patternText = pattern.startEntry.text.substring(prefixLength)
                    if (!targetEntryText.endsWith(patternText)) continue
                    targetEntryText.substring(0, targetEntryText.length - patternText.length)
                }
                else ""

                val lastTargetEntry = targetEntries[index + patternEntries.lastIndex]

                val targetSuffix = if (matchEndByText) {
                    if (lastTargetEntry !is KtLiteralStringTemplateEntry) continue

                    val patternText = with(pattern.endEntry.text) { substring(0, length - suffixLength) }
                    val lastTargetEntryText = lastTargetEntry.text
                    if (!lastTargetEntryText.startsWith(patternText)) continue
                    lastTargetEntryText.substring(patternText.length)
                }
                else ""

                val fromIndex = if (matchStartByText) 1 else 0
                val toIndex = if (matchEndByText) patternEntries.lastIndex - 1 else patternEntries.lastIndex
                val status = (fromIndex..toIndex).fold(MATCHED) { s, patternEntryIndex ->
                    val targetEntryToUnify = targetEntries[index + patternEntryIndex]
                    val patternEntryToUnify = patternEntries[patternEntryIndex]
                    if (s != UNMATCHED) s and doUnify(targetEntryToUnify, patternEntryToUnify) else s
                }
                if (status == UNMATCHED) continue
                targetSubstringInfo = ExtractableSubstringInfo(targetEntry, lastTargetEntry, targetPrefix, targetSuffix, pattern.type)
                return status
            }

            return UNMATCHED
        }

        fun doUnify(target: KotlinPsiRange, pattern: KotlinPsiRange): Status {
            (pattern.elements.singleOrNull() as? KtExpression)?.extractableSubstringInfo?.let {
                val targetTemplate = target.elements.singleOrNull() as? KtStringTemplateExpression ?: return UNMATCHED
                return doUnifyStringTemplateFragments(targetTemplate, it)
            }

            val targetElements = target.elements
            val patternElements = pattern.elements
            if (targetElements.size != patternElements.size) return UNMATCHED

            return (targetElements.asSequence().zip(patternElements.asSequence())).fold(MATCHED) { s, p ->
                if (s != UNMATCHED) s and doUnify(p.first, p.second) else s
            }
        }

        private fun ASTNode.getChildrenRange(): KotlinPsiRange =
                getChildren(null).mapNotNull { it.psi }.toRange()

        private fun PsiElement.unwrapWeakly(): KtElement? {
            return when {
                this is KtReturnExpression -> returnedExpression
                this is KtProperty -> initializer
                KtPsiUtil.isOrdinaryAssignment(this) -> (this as KtBinaryExpression).right
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

        private fun substitute(parameter: UnifierParameter, targetElement: PsiElement?): Status {
            val existingArgument = substitution[parameter]
            return when {
                existingArgument == null -> {
                    substitution[parameter] = targetElement as KtElement
                    MATCHED
                }
                else -> {
                    checkEquivalence = true
                    val status = doUnify(existingArgument, targetElement)
                    checkEquivalence = false

                    status
                }
            }
        }

        fun doUnify(
                targetElement: PsiElement?,
                patternElement: PsiElement?
        ): Status {
            val targetElementUnwrapped = targetElement?.unwrap()
            val patternElementUnwrapped = patternElement?.unwrap()

            if (targetElementUnwrapped == patternElementUnwrapped) return MATCHED
            if (targetElementUnwrapped == null || patternElementUnwrapped == null) return UNMATCHED

            if (!checkEquivalence && targetElementUnwrapped !is KtBlockExpression) {
                val referencedPatternDescriptor = when (patternElementUnwrapped) {
                    is KtReferenceExpression -> {
                        if (targetElementUnwrapped !is KtExpression) return UNMATCHED
                        patternElementUnwrapped.bindingContext[BindingContext.REFERENCE_TARGET, patternElementUnwrapped]
                    }
                    is KtUserType -> {
                        if (targetElementUnwrapped !is KtUserType) return UNMATCHED
                        patternElementUnwrapped.bindingContext[BindingContext.REFERENCE_TARGET, patternElementUnwrapped.referenceExpression]
                    }
                    else -> null
                }
                val referencedPatternDeclaration = (referencedPatternDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
                val parameter = descriptorToParameter[referencedPatternDeclaration]
                if (referencedPatternDeclaration != null && parameter != null) {
                    if (targetElementUnwrapped is KtExpression) {
                        if (!targetElementUnwrapped.checkType(parameter)) return UNMATCHED
                    }
                    else if (targetElementUnwrapped !is KtUserType) return UNMATCHED

                    return substitute(parameter, targetElementUnwrapped)
                }
            }

            val targetNode = targetElementUnwrapped.node
            val patternNode = patternElementUnwrapped.node
            if (targetNode == null || patternNode == null) return UNMATCHED

            val resolvedStatus = matchResolvedInfo(targetElementUnwrapped, patternElementUnwrapped)
            if (resolvedStatus == MATCHED) return resolvedStatus

            if (targetElementUnwrapped is KtElement && patternElementUnwrapped is KtElement) {
                val weakStatus = doUnifyWeakly(targetElementUnwrapped, patternElementUnwrapped)
                if (weakStatus != UNMATCHED) return weakStatus
            }

            if (targetNode.elementType != patternNode.elementType) return UNMATCHED

            if (resolvedStatus != null) return resolvedStatus

            val targetChildren = targetNode.getChildrenRange()
            val patternChildren = patternNode.getChildrenRange()

            if (patternChildren.empty && targetChildren.empty) {
                return if (targetElementUnwrapped.unquotedText() == patternElementUnwrapped.unquotedText()) MATCHED else UNMATCHED
            }

            return doUnify(targetChildren, patternChildren)
        }
    }

    private val descriptorToParameter = parameters.associateBy { (it.descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() }

    private fun PsiElement.unwrap(): PsiElement? {
        return when (this) {
            is KtExpression -> KtPsiUtil.deparenthesize(this)
            is KtStringTemplateEntryWithExpression -> KtPsiUtil.deparenthesize(expression)
            else -> this
        }
    }

    private fun PsiElement.unquotedText(): String {
        val text = text ?: ""
        return if (this is LeafPsiElement) KtPsiUtil.unquoteIdentifier(text) else text
    }

    fun unify(target: KotlinPsiRange, pattern: KotlinPsiRange): UnificationResult {
        return with(Context(target, pattern)) {
            val status = doUnify(target, pattern)
            when {
                substitution.size != descriptorToParameter.size ->
                    Unmatched
                status == MATCHED -> {
                    val targetRange = targetSubstringInfo?.createExpression()?.toRange() ?: target
                    if (weakMatches.isEmpty()) {
                        StronglyMatched(targetRange, substitution)
                    } else {
                        WeaklyMatched(targetRange, substitution, weakMatches)
                    }
                }
                else ->
                    Unmatched
            }
        }
    }

    fun unify(targetElement: PsiElement?, patternElement: PsiElement?): UnificationResult =
            unify(targetElement.toRange(), patternElement.toRange())
}

fun PsiElement?.matches(e: PsiElement?): Boolean = KotlinPsiUnifier.DEFAULT.unify(this, e).matched
fun KotlinPsiRange.matches(r: KotlinPsiRange): Boolean = KotlinPsiUnifier.DEFAULT.unify(this, r).matched
