// KT-45286
// MODULE: lib
// WITH_STDLIB
// FILE: A.kt

package a

import kotlin.coroutines.*

var result = "Fail"

fun f() {
    result = "OK"
}

fun g(block: suspend () -> Unit) {
    block.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

inline fun h() = g(::f)

// MODULE: main(lib)
// FILE: B.kt

package b

fun box(): String {
    a.h()
    return a.result
}
