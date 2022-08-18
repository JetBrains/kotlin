/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.element.builder.DuplicatedFirSourceElementsException
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isErrorElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

internal open class FirElementsRecorder : FirVisitor<Unit, MutableMap<KtElement, FirElement>>() {
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

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: MutableMap<KtElement, FirElement>) {
        cacheElement(variableAssignment.lValue, data) // FirReference is not cached by default
        visitElement(variableAssignment, data)
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: MutableMap<KtElement, FirElement>) {
        cacheElement(constExpression, data)
        // KtPrefixExpression(-, KtConstExpression(n)) is represented as FirConstExpression(-n) with converted constant value.
        // If one queries FIR for KtConstExpression, we still return FirConstExpression(-n) even though its source is KtPrefixExpression.
        // Here, we cache FirConstExpression(n) for KtConstExpression(n) to make everything natural and intuitive!
        if (constExpression.isConverted) {
            constExpression.kind.reverseConverted(constExpression)?.let { cacheElement(it, data) }
        }
    }

    //@formatter:off
    override fun visitReference(reference: FirReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitNamedReference(namedReference: FirNamedReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitSuperReference(superReference: FirSuperReference, data: MutableMap<KtElement, FirElement>) {}
    override fun visitThisReference(thisReference: FirThisReference, data: MutableMap<KtElement, FirElement>) {}
    //@formatter:on

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: MutableMap<KtElement, FirElement>) {
        super.visitResolvedTypeRef(errorTypeRef, data)
        errorTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: MutableMap<KtElement, FirElement>) {
        super.visitResolvedTypeRef(resolvedTypeRef, data)
        resolvedTypeRef.delegatedTypeRef?.accept(this, data)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: MutableMap<KtElement, FirElement>) {
        userTypeRef.acceptChildren(this, data)
    }

    private fun cacheElement(element: FirElement, cache: MutableMap<KtElement, FirElement>) {
        val psi = element.source
            ?.takeIf {
                it is KtRealPsiSourceElement ||
                        it.kind == KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess ||
                        it.kind == KtFakeSourceElementKind.FromUseSiteTarget ||
                        // For secondary constructors without explicit delegated constructor call, the PSI tree always create an empty
                        // KtConstructorDelegationCall. In this case, the source in FIR has this fake source kind.
                        it.kind == KtFakeSourceElementKind.ImplicitConstructor ||
                        it.kind == KtFakeSourceElementKind.DesugaredPrefixNameReference ||
                        it.kind == KtFakeSourceElementKind.DesugaredPostfixNameReference ||
                        it.kind == KtFakeSourceElementKind.SmartCastExpression ||
                        it.isSourceForCompoundAccess(element)
            }.psi as? KtElement
            ?: return
        cache(psi, element, cache)
    }

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
        if (kind != KtFakeSourceElementKind.DesugaredCompoundAssignment && kind != KtFakeSourceElementKind.DesugaredIncrementOrDecrement) return false
        return when {
            psi is KtBinaryExpression || psi is KtUnaryExpression -> fir.isWriteInCompoundCall()
            parentPsi is KtBinaryExpression && psi == parentPsi.left -> fir.isReadInCompoundCall()
            parentPsi is KtUnaryExpression && psi == parentPsi.baseExpression -> fir.isReadInCompoundCall()
            else -> false
        }
    }

    private fun FirElement.isReadInCompoundCall(): Boolean {
        if (this is FirPropertyAccessExpression) return true
        if (this !is FirFunctionCall) return false
        val name = (calleeReference as? FirResolvedNamedReference)?.name
        return name == OperatorNameConventions.GET
    }

    private fun FirElement.isWriteInCompoundCall(): Boolean {
        if (this is FirVariableAssignment) return true
        if (this !is FirFunctionCall) return false
        val name = (calleeReference as? FirResolvedNamedReference)?.name
        return name == OperatorNameConventions.SET || name in OperatorNameConventions.ASSIGNMENT_OPERATIONS
    }

    private val FirConstExpression<*>.isConverted: Boolean
        get() {
            val firSourcePsi = this.source?.psi ?: return false
            return firSourcePsi is KtPrefixExpression && firSourcePsi.operationToken == KtTokens.MINUS
        }

    private val FirConstExpression<*>.ktConstantExpression: KtConstantExpression?
        get() {
            val firSourcePsi = this.source?.psi
            return firSourcePsi?.findDescendantOfType()
        }

    private fun <T> ConstantValueKind<T>.reverseConverted(original: FirConstExpression<T>): FirConstExpression<T>? {
        val value = original.value as? Number ?: return null
        val convertedValue = when (this) {
            ConstantValueKind.Byte -> value.toByte().unaryMinus()
            ConstantValueKind.Double -> value.toDouble().unaryMinus()
            ConstantValueKind.Float -> value.toFloat().unaryMinus()
            ConstantValueKind.Int -> value.toInt().unaryMinus()
            ConstantValueKind.Long -> value.toLong().unaryMinus()
            ConstantValueKind.Short -> value.toShort().unaryMinus()
            else -> null
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        return buildConstExpression(
            original.ktConstantExpression?.toKtPsiSourceElement(),
            this,
            convertedValue as T
        ).also {
            it.replaceTypeRef(original.typeRef)
        }
    }

    companion object {
        fun recordElementsFrom(firElement: FirElement, recorder: FirElementsRecorder): Map<KtElement, FirElement> =
            buildMap { firElement.accept(recorder, this) }
    }
}
