/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

const val NO_ID: Int = 0

object BasicElementTypeId {
    const val WHITE_SPACE: Int = NO_ID + 1
    const val BAD_CHARACTER: Int = WHITE_SPACE + 1
    const val ERROR_ELEMENT: Int = BAD_CHARACTER + 1
    /**
     * Remember to update first const in [KtTokenId] after adding new tokens here ^^^
     */
}
