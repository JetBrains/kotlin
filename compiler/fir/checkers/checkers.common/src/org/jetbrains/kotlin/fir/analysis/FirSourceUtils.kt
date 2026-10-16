/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.declarations.FirImport

/**
 * Keeps 'padding' of parent node in child node
 */
internal fun KtLightSourceElement.buildChildSourceElement(childNode: LighterASTNode): KtLightSourceElement {
    val offsetDelta = startOffset - lighterASTNode.startOffset
    return childNode.toKtLightSourceElement(
        treeStructure,
        startOffset = childNode.startOffset + offsetDelta,
        endOffset = childNode.endOffset + offsetDelta
    )
}

private val IMPORT_PARENT_TOKEN_TYPES = TokenSet.create(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.REFERENCE_EXPRESSION)

/**
 * Returns a source element for the import segment that is [indexFromLast]th from last.
 */
fun FirImport.getSourceForImportSegment(indexFromLast: Int): KtSourceElement? {
    var segmentSource: KtSourceElement = source ?: return null

    repeat(indexFromLast + 1) {
        segmentSource = segmentSource.getChild(IMPORT_PARENT_TOKEN_TYPES, depth = 1) ?: return null
    }

    return segmentSource.takeIf { it.elementType == KtNodeTypes.REFERENCE_EXPRESSION }
        ?: segmentSource.getChild(KtNodeTypes.REFERENCE_EXPRESSION, depth = 1, reverse = true)
}

/**
 * Looks for the source element of the last segment
 * of `importedFqName`.
 */
fun FirImport.getLastImportedFqNameSegmentSource(): KtSourceElement? =
    source?.getChild(KtNodeTypes.REFERENCE_EXPRESSION, reverse = true)
