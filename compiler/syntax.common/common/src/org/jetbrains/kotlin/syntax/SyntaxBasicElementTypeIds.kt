/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

const val NO_ID: SyntaxId = 0

object SyntaxBasicElementTypeIds {
    const val WHITE_SPACE: SyntaxId = NO_ID + 1
    const val BAD_CHARACTER: SyntaxId = WHITE_SPACE + 1
    const val ERROR_ELEMENT: SyntaxId = BAD_CHARACTER + 1
    /**
     * Remember to update first const in [SyntaxTokenIds] after adding new tokens here ^^^
     */
}
