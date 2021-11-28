// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun <T> builder(value: T, c: suspend T.() -> Unit) {
    c.startCoroutine(value, EmptyContinuation)
}

interface A<T> {
    val value: T
    var result: T

    fun test(): T {
        builder(value) { result = this }
        return result
    }
}

fun box(): String =
    object : A<String> {
        override val value = "OK"
        override var result = "Fail"
    }.test()
