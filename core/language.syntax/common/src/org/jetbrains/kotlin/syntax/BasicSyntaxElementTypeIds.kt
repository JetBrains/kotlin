/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

/**
 * IDs of language-independent element types provided by the IntelliJ platform
 * (`com.intellij.psi.TokenType` for PSI and LightTree, `com.intellij.platform.syntax.element.SyntaxTokenTypes` for KMP).
 */
public object BasicSyntaxElementTypeIds {
    /**
     * The ID of element types that have no associated syntax element ID (e.g., external).
     */
    public const val NO_ID: Int = 0

    public const val WHITE_SPACE: Int = NO_ID + 1
    public const val BAD_CHARACTER: Int = WHITE_SPACE + 1
    public const val ERROR_ELEMENT: Int = BAD_CHARACTER + 1
    /**
     * Remember to update first const in [KotlinSyntaxTokenIds] after adding new IDs here ^^^
     */
}
