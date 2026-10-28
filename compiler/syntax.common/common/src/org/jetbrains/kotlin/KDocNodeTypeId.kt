/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

object KDocNodeTypeId {
    const val KDOC_SECTION: Int = KDocTokenId.CODE_SPAN_TEXT + 1
    const val KDOC_TAG: Int = KDOC_SECTION + 1
    const val KDOC_NAME: Int = KDOC_TAG + 1
}
