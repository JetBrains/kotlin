/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KotlinSyntaxIdSets {
    public val UNWRAPPABLE_TOKEN_TYPES: SyntaxIdSet = syntaxIdSetOf(
        KotlinSyntaxNodeTypeIds.PARENTHESIZED,
        KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION,
        KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION
    )

    public val VISIBILITY_MODIFIER: SyntaxIdSet = syntaxIdSetOf(
        KotlinSyntaxTokenIds.PRIVATE_MODIFIER,
        KotlinSyntaxTokenIds.PUBLIC_MODIFIER,
        KotlinSyntaxTokenIds.INTERNAL_MODIFIER,
        KotlinSyntaxTokenIds.PROTECTED_MODIFIER,
    )

    public val VAL_VAR: SyntaxIdSet = syntaxIdSetOf(KotlinSyntaxTokenIds.VAL_KEYWORD, KotlinSyntaxTokenIds.VAR_KEYWORD)
}
