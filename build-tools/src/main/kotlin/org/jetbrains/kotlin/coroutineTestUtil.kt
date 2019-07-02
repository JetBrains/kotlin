/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

// This is almost a full copy of kotlin/compiler/tests-common/tests/org/jetbrains/kotlin/coroutineTestUtil.kt
// TODO: get it automatically as a dependency
fun createTextForHelpers(isReleaseCoroutines: Boolean): String {
    val coroutinesPackage = "kotlin.coroutines"

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
            |class StateMachineCheckerClass {
            |    private var counter = 0
            |    var finished = false
            |
            |    var proceed: () -> Unit = {}
            |    
            |    fun reset() {
            |        counter = 0
            |        finished = false
            |        proceed = {}
            |    }
            |
            |    suspend fun suspendHere() = suspendCoroutine<Unit> { c ->
            |        counter++
            |        proceed = { c.resume(Unit) }
            |    }
            |
            |    fun check(numberOfSuspensions: Int) {
            |        for (i in 1..numberOfSuspensions) {
            |            if (counter != i) error("Wrong state-machine generated: suspendHere called should be called exactly once in one state. Expected " + i + ", got " + counter)
            |            proceed()
            |        }
            |        if (counter != numberOfSuspensions)
            |            error("Wrong state-machine generated: suspendHere called should be called exactly once in one state. Expected " + numberOfSuspensions + ", got " + counter)
            |        if (finished) error("Wrong state-machine generated: it is finished early")
            |        proceed()
            |        if (!finished) error("Wrong state-machine generated: it is not finished yet")
            |    }
            |}
            |val StateMachineChecker = StateMachineCheckerClass()
            |object CheckStateMachineContinuation: ContinuationAdapter<Unit>() {
            |    override val context: CoroutineContext
            |        get() = EmptyCoroutineContext
            |
            |    override fun resume(value: Unit) {
            |        StateMachineChecker.proceed = {
            |            StateMachineChecker.finished = true
            |        }
            |    }
            |
            |    override fun resumeWithException(exception: Throwable) {
            |        throw exception
            |    }
            |}
        """.trimMargin()
}
