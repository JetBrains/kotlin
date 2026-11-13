/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import java.lang.Deprecated

// Temporary location: `KtPsiSourceElement.syntaxId` and `KtLightSourceElement.syntaxId` need the mapping, and
// `psi-frontend-utils` can't host it since it already depends on `frontend.common`.
// Move it back to `psi-frontend-utils` once these source elements leave `frontend.common`,
// and drop the dependency of `frontend.common` on `psi-api` (KT-91461).

/**
 * @receiver A Kotlin element type
 * @return The ID from `core:language.syntax` that is consistent across all frontends (PSI/LightTree and KMP).
 * Only Kotlin tokens ([KotlinSyntaxTokenIds]), node types declared in [KotlinSyntaxNodeTypeIds], [KtTokens.DOC_COMMENT],
 * and the basic element types [TokenType.WHITE_SPACE], [TokenType.BAD_CHARACTER] and [TokenType.ERROR_ELEMENT] are mapped.
 * For other element types (e.g., KDoc element types or non-Kotlin or unregistered ones), the result is [NO_ID].
 */
@KtImplementationDetail
fun IElementType.syntaxId(): SyntaxId =
    when {
        this is KtToken -> tokenId
        index >= 0 && index < indexToId.size -> indexToId[index.toInt()]
        else -> NO_ID
    }

/**
 * The inverse of [syntaxId].
 *
 * @receiver An ID from `core:language.syntax`.
 * @return The PSI element type with the given ID, or `null` if there is no such element type (e.g., for [NO_ID]).
 * Useful for getting the name of an element (e.g., a modifier keyword for diagnostic messages) by its ID.
 * Kotlin tokens ([KtTokens]) and the element types listed in [syntaxId] are mapped.
 */
@KtImplementationDetail
fun SyntaxId.elementType(): IElementType? = idToElementType.getOrNull(this)

/**
 * Element types that are not [KtToken]s (their IDs aren't stored in the element types themselves).
 */
private val nonTokenElementTypesWithIds: List<Pair<IElementType, SyntaxId>> = buildList {
    var id = KotlinSyntaxNodeTypeIds.FILE
    for (field in KtNodeTypes::class.java.declaredFields) {
        if (field.isAnnotationPresent(Deprecated::class.java)) continue
        add((field.get(null) as IElementType) to id++)
    }
    add(KtTokens.DOC_COMMENT to KotlinSyntaxTokenIds.DOC_COMMENT)
    add(TokenType.WHITE_SPACE to BasicSyntaxElementTypeIds.WHITE_SPACE)
    add(TokenType.BAD_CHARACTER to BasicSyntaxElementTypeIds.BAD_CHARACTER)
    add(TokenType.ERROR_ELEMENT to BasicSyntaxElementTypeIds.ERROR_ELEMENT)
}

private val indexToId: IntArray = run {
    val maxIndex = nonTokenElementTypesWithIds.maxOf { it.first.index.toInt() }
    IntArray(maxIndex + 1).apply {
        for ([elementType, id] in nonTokenElementTypesWithIds) {
            this[elementType.index.toInt()] = id
        }
    }
}

private val idToElementType: Array<IElementType?> = run {
    val tokens = KtTokens::class.java.declaredFields.mapNotNull { field ->
        if (field.isAnnotationPresent(Deprecated::class.java)) return@mapNotNull null
        (field.get(null) as? KtToken)?.takeIf { it.tokenId > NO_ID }
    }
    val maxId = maxOf(nonTokenElementTypesWithIds.maxOf { it.second }, tokens.maxOf { it.tokenId })
    arrayOfNulls<IElementType>(maxId + 1).apply {
        for ([elementType, id] in nonTokenElementTypesWithIds) {
            this[id] = elementType
        }
        for (token in tokens) {
            this[token.tokenId] = token
        }
    }
}
