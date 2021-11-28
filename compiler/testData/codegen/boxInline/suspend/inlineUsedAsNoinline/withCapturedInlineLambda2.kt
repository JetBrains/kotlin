// WITH_STDLIB
// WITH_REFLECT
// WITH_COROUTINES
// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// FILE: inlined.kt
package test

interface SuspendRunnable<R> {
    suspend fun run(): R
}

inline fun makeKAdder(crossinline x: suspend () -> String) = object : SuspendRunnable<String> {
    override suspend fun run() = run { x() } + "K"
}

// FILE: box.kt
import test.*
import helpers.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun getO(): String = suspendCoroutineUninterceptedOrReturn { cont ->
    cont.resume("O")
    COROUTINE_SUSPENDED
}

var result = ""

fun box(): String {
    suspend {
        result = ((::makeKAdder as KFunction<*>).javaMethod!!.invoke(null, ::getO) as SuspendRunnable<String>).run()
    }.startCoroutine(EmptyContinuation)
    return result
}
