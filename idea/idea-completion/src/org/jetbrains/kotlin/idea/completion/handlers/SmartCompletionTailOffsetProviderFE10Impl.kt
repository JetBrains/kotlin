/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.handlers

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.completion.KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.tryGetOffset

class SmartCompletionTailOffsetProviderFE10Impl: SmartCompletionTailOffsetProvider() {
    override fun getTailOffset(context: InsertionContext, item: LookupElement): Int {
        val completionChar = context.completionChar
        var tailOffset = context.tailOffset
        if (completionChar == Lookup.REPLACE_SELECT_CHAR && item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) != null) {
            context.offsetMap.tryGetOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                ?.let { tailOffset = it }
        }
        return tailOffset
    }
}