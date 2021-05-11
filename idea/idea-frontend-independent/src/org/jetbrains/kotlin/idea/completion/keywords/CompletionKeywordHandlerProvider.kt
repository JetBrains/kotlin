/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.keywords

abstract class CompletionKeywordHandlerProvider<CONTEXT> {
    protected abstract val handlers: CompletionKeywordHandlers<CONTEXT>

    fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlers.getHandlerForKeyword(keyword)
}


class CompletionKeywordHandlers<CONTEXT>(vararg handlers: CompletionKeywordHandler<CONTEXT>) {
    private val handlerByKeyword = handlers.associateBy { it.keyword.value }

    internal fun getHandlerForKeyword(keyword: String): CompletionKeywordHandler<CONTEXT>? =
        handlerByKeyword[keyword]
}
