/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

fun createTextForHelpers(coroutinesPackage: String): String {
    return """
            |package helpers
            |import $coroutinesPackage.*
            |
            |fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
            |    override val context = EmptyCoroutineContext
            |    override fun resumeWithException(exception: Throwable) {
            |        throw exception
            |    }
            |
            |    override fun resume(data: T) = x(data)
            |}
            |
            |
            |fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
            |    override val context = EmptyCoroutineContext
            |    override fun resumeWithException(exception: Throwable) {
            |        x(exception)
            |    }
            |
            |    override fun resume(data: Any?) { }
            |}
            |
            |open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
            |    companion object : EmptyContinuation()
            |    override fun resume(data: Any?) {}
            |    override fun resumeWithException(exception: Throwable) { throw exception }
            |}
        """.trimMargin()
}
