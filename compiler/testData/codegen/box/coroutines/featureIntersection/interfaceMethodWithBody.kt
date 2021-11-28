// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend String.() -> Unit) {
    c.startCoroutine("OK", EmptyContinuation)
}

interface A {
    var result: String

    fun test(): String {
        builder { result = this }
        return result
    }
}

fun box(): String =
    object : A {
        override var result = "Fail"
    }.test()
