/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.resolve.DescriptorUtils

fun createTextForHelpers(isReleaseCoroutines: Boolean): String {
    val coroutinesPackage =
        if (isReleaseCoroutines)
            DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE.asString()
        else
            DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.asString()

    val emptyContinuationBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<Any?>) {
                |   result.getOrThrow()
                |}
            """.trimMargin()
        else
            """
                |override fun resume(data: Any?) {}
                |override fun resumeWithException(exception: Throwable) { throw exception }
            """.trimMargin()

    val handleResultContinuationBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<T>) {
                |   x(result.getOrThrow())
                |}
            """.trimMargin()
        else
            """
                |override fun resumeWithException(exception: Throwable) {
                |   throw exception
                |}
                |
                |override fun resume(data: T) = x(data)
            """.trimMargin()

    val handleExceptionContinuationBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<Any?>) {
                |   result.exceptionOrNull()?.let(x)
                |}
            """.trimMargin()
        else
            """
                |override fun resumeWithException(exception: Throwable) {
                |   x(exception)
                |}
                |
                |override fun resume(data: Any?) {}
            """.trimMargin()

    val continuationAdapterBody =
        if (isReleaseCoroutines)
            """
                |override fun resumeWith(result: Result<T>) {
                |   if (result.isSuccess) {
                |       resume(result.getOrThrow())
                |   } else {
                |       resumeWithException(result.exceptionOrNull()!!)
                |   }
                |}
                |
                |abstract fun resumeWithException(exception: Throwable)
                |abstract fun resume(value: T)
            """.trimMargin()
        else
            ""

    return """
            |package helpers
            |import $coroutinesPackage.*
            |
            |fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
            |    override val context = EmptyCoroutineContext
            |    $handleResultContinuationBody
            |}
            |
            |
            |fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
            |    override val context = EmptyCoroutineContext
            |    $handleExceptionContinuationBody
            |}
            |
            |open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
            |    companion object : EmptyContinuation()
            |    $emptyContinuationBody
            |}
            |
            |abstract class ContinuationAdapter<in T> : Continuation<T> {
            |    override val context: CoroutineContext = EmptyCoroutineContext
            |    $continuationAdapterBody
            |}
        """.trimMargin()
}
