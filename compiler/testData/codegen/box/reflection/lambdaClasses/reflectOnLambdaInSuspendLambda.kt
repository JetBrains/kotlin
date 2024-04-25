// LAMBDAS: CLASS
// OPT_IN: kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES
// FILE: a.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.reflect.jvm.reflect

fun box(): String {
    lateinit var x: (String) -> Unit
    suspend {
        x = { OK: String -> }
    }.startCoroutine(EmptyContinuation)
    return x.reflect()?.parameters?.singleOrNull()?.name ?: "null"
}
