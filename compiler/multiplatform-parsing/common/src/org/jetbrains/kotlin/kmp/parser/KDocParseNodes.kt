/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
object KDocParseNodes {
    val KDOC_SECTION: SyntaxElementType = SyntaxElementType("KDOC_SECTION")
    val KDOC_TAG: SyntaxElementType = SyntaxElementType("KDOC_TAG")
    val KDOC_NAME: SyntaxElementType = SyntaxElementType("KDOC_NAME")
}