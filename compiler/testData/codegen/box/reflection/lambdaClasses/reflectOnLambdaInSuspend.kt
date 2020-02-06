// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES
// FILE: a.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.reflect.jvm.reflect

suspend fun f() = { OK: String -> }

fun box(): String {
    lateinit var x: (String) -> Unit
    suspend {
        x = f()
    }.startCoroutine(EmptyContinuation)
    return x.reflect()?.parameters?.singleOrNull()?.name ?: "null"
}
