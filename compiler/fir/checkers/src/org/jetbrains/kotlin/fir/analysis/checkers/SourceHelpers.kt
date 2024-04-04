/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.findDescendantByType
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull


/**
 * Returns Visibility by token or null
 */
fun KtModifierKeywordToken.toVisibilityOrNull(): Visibility? {
    return when (this) {
        KtTokens.PUBLIC_KEYWORD -> Visibilities.Public
        KtTokens.PRIVATE_KEYWORD -> Visibilities.Private
        KtTokens.PROTECTED_KEYWORD -> Visibilities.Protected
        KtTokens.INTERNAL_KEYWORD -> Visibilities.Internal
        else -> null
    }
}

/**
 * Locates first [CONTEXT_RECEIVER_LIST] and returns position in source.
 */
fun KtSourceElement.findContextReceiverListSource(): KtLightSourceElement? {
    if (this.lighterASTNode.tokenType == KtNodeTypes.CONTEXT_RECEIVER_LIST)
        return this.lighterASTNode.toKtLightSourceElement(treeStructure)

    return treeStructure.findDescendantByType(
        lighterASTNode,
        KtNodeTypes.CONTEXT_RECEIVER_LIST,
        false
    )?.toKtLightSourceElement(treeStructure)
}

internal fun KtSourceElement.delegatedPropertySourceOrThis(context: CheckerContext): KtSourceElement {
    if (kind == KtFakeSourceElementKind.DelegatedPropertyAccessor) {
        val property = context.containingDeclarations.lastIsInstanceOrNull<FirProperty>()
        property?.delegate?.source?.fakeElement(KtFakeSourceElementKind.DelegatedPropertyAccessor)?.let { return it }
    }

    return this
}