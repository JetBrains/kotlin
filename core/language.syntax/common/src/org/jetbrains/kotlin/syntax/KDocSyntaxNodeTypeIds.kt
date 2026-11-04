/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KDocSyntaxNodeTypeIds {
    public const val KDOC_SECTION: SyntaxId = KDocSyntaxTokenIds.CODE_SPAN_TEXT + 1
    public const val KDOC_TAG: SyntaxId = KDOC_SECTION + 1
    public const val KDOC_NAME: SyntaxId = KDOC_TAG + 1
}
