/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularPropertySymbol
import org.jetbrains.kotlin.fir.syntax.SyntaxIdPredicate
import org.jetbrains.kotlin.fir.syntax.getChild
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.syntax.KotlinSyntaxIdSets
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.syntax.SyntaxIdSet

/**
 * Returns the start offset of the first direct child matching [keywordTokens] (if specified and there is such a child),
 * otherwise, the start offset of the element skipping its leading comments.
 */
context(c: Fir2IrComponents)
internal fun KtSourceElement.startOffsetSkippingComments(keywordTokens: SyntaxIdSet? = null): Int {
    return withSyntax {
        keywordTokens?.let { getChild(it, depth = 1) }?.startOffset ?: getStartOffsetSkippingComments()
    }
}

context(c: Fir2IrComponents)
internal inline fun <T : IrElement> FirElement.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    val tokenSet = when (this) {
        is FirNamedFunction -> FUNCTION_KEYWORD_TOKENS
        is FirConstructor -> CONSTRUCTOR_KEYWORD_TOKENS
        is FirVariable -> KotlinSyntaxIdSets.VAL_VAR
        else -> null
    }
    return source.convertWithOffsets(tokenSet, f)
}

context(c: Fir2IrComponents)
internal fun <T : IrElement> FirPropertyAccessor?.convertWithOffsets(
    defaultStartOffset: Int,
    defaultEndOffset: Int,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    if (this == null) return f(defaultStartOffset, defaultEndOffset)

    /**
     * Default accessors should have offsets of the property declaration itself, without the property initializer (KT-69911)
     *
     * ```
     * <1>val property: String<2> = "aaa"<3>
     * ```
     * Property offsets: <1>..<3>
     * Accessors offsets: <1>..<2>
     */
    if (source?.kind is KtFakeSourceElementKind.DefaultAccessor) {
        val property = this.propertySymbol.fir
        if (property.symbol is FirRegularPropertySymbol) {
            property.computeOffsetsWithoutInitializer()?.let { [startOffset, endOffset] ->
                return f(startOffset, endOffset)
            }
        }
    }

    return source.convertWithOffsets(KotlinSyntaxIdSets.VAL_VAR, f)
}

context(c: Fir2IrComponents)
internal inline fun <T : IrElement> KtSourceElement?.convertWithOffsets(
    keywordTokens: SyntaxIdSet?,
    f: (startOffset: Int, endOffset: Int) -> T,
): T {
    val startOffset: Int
    val endOffset: Int

    if (
        isCompiledElement(psi) ||
        this?.kind is KtFakeSourceElementKind.DataClassGeneratedMembers ||
        this?.kind == KtFakeSourceElementKind.ImplicitThisReceiverExpression ||
        this?.kind == KtFakeSourceElementKind.ImplicitContextParameterArgument
    ) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = this?.startOffsetSkippingComments(keywordTokens) ?: UNDEFINED_OFFSET
        endOffset = this?.endOffset ?: UNDEFINED_OFFSET
    }

    return f(startOffset, endOffset)
}

context(c: Fir2IrComponents)
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

context(c: Fir2IrComponents)
internal inline fun <T : IrElement> FirThisReceiverExpression.convertWithOffsets(f: (startOffset: Int, endOffset: Int) -> T): T {
    return source.convertWithOffsets(keywordTokens = null, f)
}

context(c: Fir2IrComponents)
internal inline fun <T : IrElement> FirStatement.convertWithOffsets(
    calleeReference: FirReference,
    f: (startOffset: Int, endOffset: Int) -> T
): T {
    val startOffset: Int
    val endOffset: Int
    if (isCompiledElement(psi) || calleeReference.source?.kind == KtFakeSourceElementKind.ImplicitContextParameterArgument) {
        startOffset = UNDEFINED_OFFSET
        endOffset = UNDEFINED_OFFSET
    } else {
        startOffset = calleeReference.source?.startOffsetSkippingComments() ?: UNDEFINED_OFFSET
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

/**
 * Returns the offsets of the property declaration up to (not including) `=` before its initializer,
 * or `null` if the property has no initializer.
 */
context(c: Fir2IrComponents)
private fun FirProperty.computeOffsetsWithoutInitializer(): Pair<Int, Int>? {
    val propertySource = source ?: return null
    if (initializer?.source == null) return null

    var endOffset = UNDEFINED_OFFSET
    withSyntax {
        propertySource.forEachChildMatching(depth = 1, reverse = false, NON_FILLER_TOKENS) {
            if (it.syntaxId == KotlinSyntaxTokenIds.EQ) return@forEachChildMatching false
            endOffset = it.endOffset
            true
        }
    }
    if (endOffset == UNDEFINED_OFFSET) return null

    val startOffset = propertySource.startOffsetSkippingComments(KotlinSyntaxIdSets.VAL_VAR)
    return startOffset to endOffset
}

private val CONSTRUCTOR_KEYWORD_TOKENS = SyntaxIdSet.of(KotlinSyntaxTokenIds.CONSTRUCTOR_KEYWORD)
private val FUNCTION_KEYWORD_TOKENS = SyntaxIdSet.of(KotlinSyntaxTokenIds.FUN_MODIFIER)
private val NON_FILLER_TOKENS = SyntaxIdPredicate { it !in KotlinSyntaxIdSets.WHITE_SPACE_OR_COMMENT_BIT_SET }
