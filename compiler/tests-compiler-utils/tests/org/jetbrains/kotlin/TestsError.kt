/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import java.io.PrintStream
import java.io.PrintWriter

enum class TestsExceptionType(val postfix: String) {
    COMPILER_ERROR("compiler"),
    COMPILETIME_ERROR("compiletime"),
    RUNTIME_ERROR("runtime"),
    INFRASTRUCTURE_ERROR("infrastructure");

    companion object {
        private val map = values().associateBy(TestsExceptionType::postfix)

        fun fromValue(type: String) = map[type]
    }
}

sealed class TestsError(val original: Throwable, val type: TestsExceptionType) : Error() {
    override fun toString(): String = original.toString()
    override fun getStackTrace(): Array<out StackTraceElement> = original.stackTrace
    override fun initCause(cause: Throwable?): Throwable = original.initCause(cause)
    override val cause: Throwable? get() = original.cause

    // This function is called in the constructor of Throwable, where original is not yet initialized
    override fun fillInStackTrace(): Throwable? = @Suppress("UNNECESSARY_SAFE_CALL", "SAFE_CALL_WILL_CHANGE_NULLABILITY") original?.fillInStackTrace()

    override fun setStackTrace(stackTrace: Array<out StackTraceElement>?) {
        original.stackTrace = stackTrace
    }

    override fun printStackTrace() = original.printStackTrace()
    override fun printStackTrace(s: PrintStream?) = original.printStackTrace(s)
    override fun printStackTrace(s: PrintWriter?) = original.printStackTrace(s)
}

class TestsCompilerError(original: Throwable) : TestsError(original, TestsExceptionType.COMPILER_ERROR)
class TestsInfrastructureError(original: Throwable) : TestsError(original, TestsExceptionType.INFRASTRUCTURE_ERROR)
class TestsCompiletimeError(original: Throwable) : TestsError(original, TestsExceptionType.COMPILETIME_ERROR)
class TestsRuntimeError(original: Throwable) : TestsError(original, TestsExceptionType.RUNTIME_ERROR)
