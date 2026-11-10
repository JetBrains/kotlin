/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.SyntaxIdSet
import org.jetbrains.kotlin.syntax.syntaxIdSetOf

context(syntax: FirSyntaxComponent<*>)
fun KtSourceElement.getChild(type: SyntaxId, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChildMatching(index, depth, reverse) { it == type }

context(syntax: FirSyntaxComponent<*>)
fun KtSourceElement.getChild(types: SyntaxIdSet, index: Int = 0, depth: Int = -1, reverse: Boolean = false): KtSourceElement? =
    getChildMatching(index, depth, reverse) { it in types }

context(syntax: FirSyntaxComponent<*>)
private fun KtSourceElement.getChildMatching(
    index: Int,
    depth: Int,
    reverse: Boolean,
    isTargetType: (SyntaxId) -> Boolean,
): KtSourceElement? {
    var idx = index
    var result: KtSourceElement? = null

    with(syntax) {
        forEachChildMatching(depth, reverse, isTargetType) {
            if (idx-- == 0) {
                result = it
                false
            } else {
                true
            }
        }
    }

    return result
}

/**
 * Iterates recursively over all children up to the given [depth] or indefinitely if [depth] is -1.
 * [processChild] is invoked for each child having a type in the [types] set.
 */
context(syntax: FirSyntaxComponent<*>)
fun KtSourceElement.forEachChildOfType(
    types: SyntaxIdSet,
    depth: Int,
    reverse: Boolean = false,
    processChild: (KtSourceElement) -> Unit,
) {
    with(syntax) {
        forEachChildMatching(depth, reverse, isTargetType = { it in types }) {
            processChild(it)
            true
        }
    }
}

private val IMPORT_PARENT_TOKEN_TYPES =
    syntaxIdSetOf(KotlinSyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION, KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION)

/**
 * Returns a source element for the import segment that is [indexFromLast]th from last.
 */
context(syntax: FirSyntaxComponent<*>)
fun FirImport.getSourceForImportSegment(indexFromLast: Int): KtSourceElement? {
    var segmentSource: KtSourceElement = source ?: return null

    repeat(indexFromLast + 1) {
        segmentSource = segmentSource.getChild(IMPORT_PARENT_TOKEN_TYPES, depth = 1) ?: return null
    }

    return segmentSource.takeIf { it.syntaxId == KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION }
        ?: segmentSource.getChild(KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION, depth = 1, reverse = true)
}

/**
 * Looks for the source element of the last segment
 * of `importedFqName`.
 */
context(syntax: FirSyntaxComponent<*>)
fun FirImport.getLastImportedFqNameSegmentSource(): KtSourceElement? =
    source?.getChild(KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION, reverse = true)
