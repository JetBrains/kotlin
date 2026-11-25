/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

/**
 * Tree-agnostic counterparts of PSI token sets (e.g., `KtTokens.MODALITY_MODIFIERS`, `psiUtil.UNWRAPPABLE_TOKEN_TYPES`).
 * Each set must contain the same elements as its PSI counterpart, which can't be referenced here.
 */
object KotlinSyntaxIdSets {
    val UNWRAPPABLE_TOKEN_TYPES: SyntaxIdSet = SyntaxIdSet.of(
        KotlinSyntaxNodeTypeIds.PARENTHESIZED,
        KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION,
        KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION
    )

    val VISIBILITY_MODIFIERS: SyntaxIdSet = SyntaxIdSet.of(
        KotlinSyntaxTokenIds.PRIVATE_MODIFIER,
        KotlinSyntaxTokenIds.PUBLIC_MODIFIER,
        KotlinSyntaxTokenIds.INTERNAL_MODIFIER,
        KotlinSyntaxTokenIds.PROTECTED_MODIFIER,
    )

    val MODALITY_MODIFIERS: SyntaxIdSet = SyntaxIdSet.of(
        KotlinSyntaxTokenIds.ABSTRACT_MODIFIER,
        KotlinSyntaxTokenIds.FINAL_MODIFIER,
        KotlinSyntaxTokenIds.SEALED_MODIFIER,
        KotlinSyntaxTokenIds.OPEN_MODIFIER,
    )
}
