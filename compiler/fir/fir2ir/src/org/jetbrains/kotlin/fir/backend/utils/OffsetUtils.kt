/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.startOffsetSkippingComments
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments

fun AbstractKtSourceElement?.startOffsetSkippingComments(): Int? {
    return when (this) {
        is KtPsiSourceElement -> this.psi.startOffsetSkippingComments
        is KtLightSourceElement -> this.startOffsetSkippingComments
        else -> null
    }
}

internal inline fun <T : IrElement> FirElement.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    return source.convertWithOffsets(f)
}

internal fun <T : IrElement> FirPropertyAccessor?.convertWithOffsets(
    defaultStartOffset: Int,
    defaultEndOffset: Int,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    return if (this == null) return f(defaultStartOffset, defaultEndOffset) else source.convertWithOffsets(f)
}

internal inline fun <T : IrElement> KtSourceElement?.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    val startOffset: Int
    val endOffset: Int

    if (
        isCompiledElement(psi) ||
        this?.kind == KtFakeSourceElementKind.DataClassGeneratedMembers ||
        this?.kind == KtFakeSourceElementKind.ImplicitThisReceiverExpression
    ) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = this?.startOffsetSkippingComments() ?: this?.startOffset ?: UNDEFINED_OFFSET
        endOffset = this?.endOffset ?: UNDEFINED_OFFSET
    }

    return f(startOffset, endOffset)
}

internal fun <T : IrElement> FirQualifiedAccessExpression.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    if (shouldUseCalleeReferenceAsItsSourceInIr()) {
        return convertWithOffsets(calleeReference, f)
    }
    return (this as FirElement).convertWithOffsets(f)
}

/**
 * This function determines which source should be used for IR counterpart of this FIR expression.
 *
 * At the moment, this function reproduces (~) K1 logic.
 * Currently, K1 uses full qualified expression source (from receiver to selector)
 * in case of an operator call, an infix call, a callable reference, or a referenced class/object.
 * Otherwise, only selector is used as a source.
 *
 * See also KT-60111 about an operator call case (xxx + yyy).
 */
fun FirQualifiedAccessExpression.shouldUseCalleeReferenceAsItsSourceInIr(): Boolean {
    return when {
        this is FirImplicitInvokeCall -> true
        this is FirFunctionCall && origin != FirFunctionCallOrigin.Regular -> false
        this is FirCallableReferenceAccess -> false
        else -> (calleeReference as? FirResolvedNamedReference)?.resolvedSymbol is FirCallableSymbol
    }
}

internal inline fun <T : IrElement> FirThisReceiverExpression.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    return source.convertWithOffsets(f)
}

internal inline fun <T : IrElement> FirStatement.convertWithOffsets(
    calleeReference: FirReference,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val startOffset: Int
    val endOffset: Int
    if (isCompiledElement(psi)) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = calleeReference.source?.startOffsetSkippingComments() ?: calleeReference.source?.startOffset ?: UNDEFINED_OFFSET
        endOffset = source?.endOffset ?: UNDEFINED_OFFSET
    }
    return f(startOffset, endOffset)
}

private fun isCompiledElement(element: PsiElement?): Boolean {
    if (element == null) {
        return false
    }

    if (element is PsiCompiledElement) {
        return true
    }

    val containingFile = element.containingFile
    return containingFile !is KtFile || containingFile.isCompiled
}
