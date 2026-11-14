/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.syntax.KDocSyntaxNodeTypeIds

@ApiStatus.Experimental
object KDocParseNodes : SyntaxElementTypesWithIds() {
    val KDOC_SECTION: SyntaxElementType = register(KDocSyntaxNodeTypeIds.KDOC_SECTION, "KDOC_SECTION")
    val KDOC_TAG: SyntaxElementType = register(KDocSyntaxNodeTypeIds.KDOC_TAG, "KDOC_TAG")
    val KDOC_NAME: SyntaxElementType = register(KDocSyntaxNodeTypeIds.KDOC_NAME, "KDOC_NAME")
}
