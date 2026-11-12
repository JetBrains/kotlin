/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.DuplicatedFirSourceElementsException
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isErrorElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.StandardTypes
import org.jetbrains.kotlin.fir.builder.toFirOperationOrNull
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Records the FIR element for each PSI element of the visited FIR tree (see [recordElementsFrom]), which [KtToFirMapping] then reads.
 *
 * A FIR element is recorded for its [anchorPsi]. A PSI element without a FIR element of its own, such as the integer literal inside a
 * literal folded with unary signs, is mapped to a related or a synthetic FIR element instead.
 */
internal open class FirElementsRecorder : FirVisitor<Unit, MutableMap<KtElement, FirElement>>() {

    /**
     * Note: generally, each FIR element with a `KtRealPsiSourceElement` source should be mapped to a unique PSI element.
     * If multiple FIR elements have the same real source PSI element, it is probably a bug in Raw FIR building code.
     */
    private fun cache(psi: KtElement, fir: FirElement, cache: MutableMap<KtElement, FirElement>) {
        val existingFir = cache[psi]
        if (existingFir != null && existingFir !== fir) {
            when {
                existingFir is FirTypeRef && fir is FirTypeRef && psi is KtTypeReference -> {
                    // FirTypeRefs are often created during resolve
                    // a lot of them with have the same source
                    // we want to take the most "resolved one" here
                    if (fir is FirResolvedTypeRefImpl && existingFir !is FirResolvedTypeRefImpl) {
                        cache[psi] = fir
                    }
                }
                existingFir.isErrorElement && !fir.isErrorElement -> {
                    // TODO better handle error elements
                    // but for now just take first non-error one if such exist
                    cache[psi] = fir
                }
                existingFir.isErrorElement || fir.isErrorElement -> {
                    // do nothing and maybe upgrade to a non-error element in the branch above in the future
                }
                else -> {
                    if (DuplicatedFirSourceElementsException.IS_ENABLED) {
                        throw DuplicatedFirSourceElementsException(existingFir, fir, psi)
                    }
                }
            }
        }
        if (existingFir == null) {
            cache[psi] = fir
        }
    }

    override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
        cacheElement(element, data)
        element.acceptChildren(this, data)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: MutableMap<KtElement, FirElement>) {
        for (bound in typeParameter.bounds) {
            val constraintSubject = (bound.psi?.parent as? KtTypeConstraint)?.subjectTypeParameterName ?: continue
            cache(constraintSubject, typeParameter, data)
        }
        super.visitTypeParameter(typeParameter, data)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: MutableMap<KtElement, FirElement>) {
        // For the LHS of the assignment, record the assignment itself
        (variableAssignment.lValue.source?.psi as? KtElement)?.let { cache(it, variableAssignment, data) }
        visitElement(variableAssignment, data)
    }

    override fun visitLiteralExpression(literalExpression: FirLiteralExpression, data: MutableMap<KtElement, FirElement>) {
        literalExpression.annotations.forEach {
            it.accept(this, data)
        }

        when (val psi = literalExpression.anchorPsi as? KtElement) {
            null -> {}
            is KtPrefixExpression -> cacheFoldedLiteral(psi, literalExpression, data)
            else -> cache(psi, literalExpression, data)
        }
    }

    /**
     * Caches [foldedLiteral], into which raw FIR folds an integer literal together with the unary minuses and pluses around it,
     * such as `-(+1)` with [prefixExpression] as its source.
     *
     * Every prefix expression of the chain is mapped to [foldedLiteral], and [KtToFirMapping] represents each of them as a fake
     * operator call of the folded literal's type (KT-70774).
     * The integer literal is mapped to a copy with its own value, see [withoutSigns].
     */
    private fun cacheFoldedLiteral(
        prefixExpression: KtPrefixExpression,
        foldedLiteral: FirLiteralExpression,
        data: MutableMap<KtElement, FirElement>,
    ) {
        val prefixExpressions = mutableListOf<KtPrefixExpression>()
        var current: PsiElement? = prefixExpression
        while (current is KtPrefixExpression) {
            prefixExpressions += current
            current = current.baseExpression.unwrapParenthesesLabelsAndAnnotations()
        }

        val constantExpression = current as? KtConstantExpression ?: return
        val isNegated = prefixExpressions.count { it.operationToken == KtTokens.MINUS } % 2 == 1
        val literalWithoutSigns = foldedLiteral.withoutSigns(isNegated, constantExpression.toKtPsiSourceElement()) ?: return
        cache(constantExpression, literalWithoutSigns, data)

        for (expression in prefixExpressions) {
            cache(expression, foldedLiteral, data)
        }
    }

    /**
     * Returns a copy of this folded literal for the integer literal inside it, with the value negated if [isNegated].
     *
     * A value that doesn't fit the folded kind, such as `2147483648` in `-2147483648` or `128` in `val b: Byte = -128`,
     * gets the `Int` or the `Long` kind.
     */
    private fun FirLiteralExpression.withoutSigns(isNegated: Boolean, source: KtSourceElement): FirLiteralExpression? {
        if (!isNegated) return buildResolvedLiteral(source, kind, value, resolvedType)

        val negatedValue = (value as? Long)?.let { -it } ?: return null
        val kindWithoutSigns = when (kind) {
            ConstantValueKind.IntegerLiteral, ConstantValueKind.Long -> kind
            ConstantValueKind.Byte -> if (negatedValue in Byte.MIN_VALUE..Byte.MAX_VALUE) kind else ConstantValueKind.Int
            ConstantValueKind.Short -> if (negatedValue in Short.MIN_VALUE..Short.MAX_VALUE) kind else ConstantValueKind.Int
            ConstantValueKind.Int -> if (negatedValue in Int.MIN_VALUE..Int.MAX_VALUE) kind else ConstantValueKind.Long
            else -> return null
        }

        val typeWithoutSigns = when (kindWithoutSigns) {
            kind -> resolvedType
            ConstantValueKind.Int -> StandardTypes.Int
            else -> StandardTypes.Long
        }

        return buildResolvedLiteral(source, kindWithoutSigns, negatedValue, typeWithoutSigns)
    }

    private fun buildResolvedLiteral(
        source: KtSourceElement,
        kind: ConstantValueKind,
        value: Any?,
        type: ConeKotlinType,
    ): FirLiteralExpression = buildLiteralExpression(source, kind, value, setType = false).also {
        it.replaceConeTypeOrNull(type)
    }

    //@formatter:off
    override fun visitReference(reference: FirReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitNamedReference(namedReference: FirNamedReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitThisReference(thisReference: FirThisReference, data: MutableMap<KtElement, FirElement>) {}
    //@formatter:on

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: MutableMap<KtElement, FirElement>) {
        super.visitResolvedTypeRef(errorTypeRef, data)
        recordTypeQualifiers(errorTypeRef, data)
        errorTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: MutableMap<KtElement, FirElement>) {
        super.visitResolvedTypeRef(resolvedTypeRef, data)
        recordTypeQualifiers(resolvedTypeRef, data)
        resolvedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: MutableMap<KtElement, FirElement>) {
        userTypeRef.acceptChildren(this, data)
    }

    protected fun cacheElement(element: FirElement, cache: MutableMap<KtElement, FirElement>) {
        val psi = element.anchorPsi as? KtElement ?: return
        cache(psi, element, cache)
    }

    private fun recordTypeQualifiers(resolvedTypeRef: FirResolvedTypeRef, data: MutableMap<KtElement, FirElement>) {
        val userTypeRef = resolvedTypeRef.delegatedTypeRef as? FirUserTypeRef ?: return
        val qualifiers = userTypeRef.qualifier
        if (qualifiers.size <= 1) return
        qualifiers.forEachIndexed { index, qualifierPart ->
            if (index == qualifiers.lastIndex) return@forEachIndexed
            val source = qualifierPart.source?.psi as? KtElement ?: return@forEachIndexed
            cache(source, resolvedTypeRef, data)
        }
    }

    companion object {
        fun recordElementsFrom(firElement: FirElement, recorder: FirElementsRecorder): Map<KtElement, FirElement> =
            buildMap { firElement.accept(recorder, this) }

        /**
         * The PSI element which can be used as an anchor point for FIR <–> PSI mapping.
         *
         * Not all fake FIR elements might have an anhor PSI element to avoid conflict with the original source element.
         * For instance, the synthetic enum supertype would have the same psi as the class itself, so it shouldn't be used
         * as an anchor to avoid ambiguity. Clients won't expect to see the supertype type reference value instead of the [FirRegularClass][org.jetbrains.kotlin.fir.declarations.FirRegularClass]
         * by [KtClass] key.
         */
        val FirElement.anchorPsi: PsiElement?
            get() {
                val source = source as? KtPsiSourceElement? ?: return null
                when (source.kind) {
                    KtRealSourceElementKind,
                    KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
                    KtFakeSourceElementKind.FromUseSiteTarget,
                        // To allow type retrieval from erroneous typealias even though it is erroneous
                    KtFakeSourceElementKind.ErroneousTypealiasExpansion,
                        // For secondary constructors without explicit delegated constructor call, the PSI tree always create an empty
                        // KtConstructorDelegationCall. In this case, the source in FIR has this fake source kind.
                    KtFakeSourceElementKind.ImplicitConstructor,
                    KtFakeSourceElementKind.DanglingModifierList,

                        /**
                         * The [FirCodeFragment][org.jetbrains.kotlin.fir.declarations.FirCodeFragment]'s real source is the
                         * [KtCodeFragment], which also happens to be the [KtFile]. So the FIR code fragment shares a real source with its
                         * containing FIR file. We still need to record code fragments here so that the [KtCodeFragment] can be mapped to
                         * `FirCodeFragment`.
                         *
                         * Despite sharing a real source with the [FirFile][org.jetbrains.kotlin.fir.declarations.FirFile], this doesn't
                         * introduce ambiguities. The FIR file is accessed via [getOrBuildFirForKtFile][org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder.getOrBuildFirForKtFile]
                         * or [getOrBuildFirFile][org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade.getOrBuildFirFile],
                         * while the FIR code fragment is accessed via [getFirForNonKtFileElement][org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.FirElementBuilder.getFirForNonKtFileElement].
                         */
                    KtFakeSourceElementKind.CodeFragment.CodeFragmentDeclaration,
                        -> Unit

                    else if (
                            source.isSourceForSmartCasts(this) ||
                                    source.isSourceForArrayAugmentedAssign(this) ||
                                    source.isSourceForCompoundAccess(this) ||
                                    source.isSourceForInvertedInOperator(this)
                            )
                        -> Unit

                    else -> return null
                }

                return source.psi
            }

        private fun KtSourceElement.isSourceForInvertedInOperator(fir: FirElement) =
            kind == KtFakeSourceElementKind.DesugaredInvertedContains
                    && fir is FirResolvedNamedReference && fir.name == OperatorNameConventions.CONTAINS

        /**
         * FIR represents compound assignment and inc/dec operations as multiple smaller instructions. Here we choose the write operation as the
         * resolved FirElement for binary and unary expressions. For example, the `FirVariableAssignment` or the call to `set` or `plusAssign`
         * function, etc. This is because the write FirElement can be used to retrieve all other information related to this compound operation.

         * On the other hand, if the PSI is the left operand of an assignment or the base expression of a unary expression, we take the read FIR
         * element so the user of the Analysis API is able to retrieve such read calls reliably.
         */
        private fun KtSourceElement.isSourceForCompoundAccess(fir: FirElement): Boolean {
            val psi = psi
            val parentPsi = psi?.parent
            if (kind !is KtFakeSourceElementKind.DesugaredAugmentedAssign && kind !is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
                return false
            }
            return when {
                psi is KtBinaryExpression || psi is KtUnaryExpression -> fir.isWriteInCompoundCall()
                parentPsi is KtBinaryExpression && psi == parentPsi.left -> fir.isReadInCompoundCall()
                parentPsi is KtUnaryExpression && psi == parentPsi.baseExpression -> fir.isReadInCompoundCall()
                else -> false
            }
        }

        /**
         * After desugaring, we also have FirBlock with the same source element.
         * We need to filter it out to map this source element to set/plusAssign call, so we check `is FirFunctionCall`.
         *
         * **Note:** even real source elements might be marked as fake ones.

         * ```kotlin
         * object ClientTransaction {
         *     operator fun plusAssign(a: Int) {}
         * }
         *
         * fun main() {
         *     ClientTransaction += 10
         * }
         * ```
         *
         * Here `ClientTransaction` is marked as `DesugaredAugmentedAssign` even though it is a real source element ([FirResolvedQualifier]).
         */
        private fun KtSourceElement.isSourceForArrayAugmentedAssign(fir: FirElement): Boolean {
            return kind is KtFakeSourceElementKind.DesugaredAugmentedAssign && (fir is FirFunctionCall || fir is FirThisReceiverExpression || fir is FirResolvedQualifier)
        }

        // `FirSmartCastExpression` forward the source from the original expression,
        // and implicit receivers have fake sources pointing to a wider part of the expression.
        // Thus, `FirElementsRecorder` may try assigning an unnecessarily wide source
        // to smart cast expressions, which will affect the
        // `org.jetbrains.kotlin.idea.highlighting.highlighters.ExpressionsSmartcastHighlighter#highlightExpression`
        // function in intellij.git
        private fun KtSourceElement.isSourceForSmartCasts(fir: FirElement) =
            (kind is KtFakeSourceElementKind.SmartCastExpression) && fir is FirSmartCastExpression && !fir.originalExpression.isImplicitThisReceiver

        private val FirExpression.isImplicitThisReceiver get() = this is FirThisReceiverExpression && this.isImplicit

        private fun FirElement.isReadInCompoundCall(): Boolean {
            if (this is FirPropertyAccessExpression) return true
            if (this !is FirFunctionCall) return false
            val name = (calleeReference as? FirResolvedNamedReference)?.name ?: getFallbackCompoundCalleeName()
            return name == OperatorNameConventions.GET
        }

        private fun FirElement.isWriteInCompoundCall(): Boolean {
            if (this is FirVariableAssignment) return true
            if (this !is FirFunctionCall) return false
            val name = (calleeReference as? FirResolvedNamedReference)?.name ?: getFallbackCompoundCalleeName()
            return name == OperatorNameConventions.SET || name in OperatorNameConventions.ASSIGNMENT_OPERATIONS
        }

        /**
         * If the callee reference is not a [FirResolvedNamedReference], we can get the compound callee name from the source instead. For
         * example, if the callee reference is a [FirErrorNamedReference] with an unresolved name `plusAssign`, the operation element type from
         * the source will be `KtTokens.PLUSEQ`, which can be transformed to `plusAssign`.
         */
        private fun FirElement.getFallbackCompoundCalleeName(): Name? {
            val psi = source.psi as? KtOperationExpression ?: return null
            val operationReference = psi.operationReference
            return operationReference.getAssignmentOperationName() ?: operationReference.getReferencedNameAsName()
        }

        private fun KtSimpleNameExpression.getAssignmentOperationName(): Name? {
            val firOperation = getReferencedNameElementType().toFirOperationOrNull() ?: return null
            return FirOperationNameConventions.ASSIGNMENTS[firOperation]
        }
    }
}
