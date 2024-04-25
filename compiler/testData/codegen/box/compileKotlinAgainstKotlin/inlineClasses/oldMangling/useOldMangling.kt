// LANGUAGE: +InlineClasses
// WITH_STDLIB

// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: 1.kt
package test

inline class IC(val s: String)

fun ordinary(s: String, ic: IC): String = s + ic.s

suspend fun suspend(s: String, ic: IC): String = s + ic.s

// MODULE: main(lib)
// FILE: 2.kt
import kotlin.coroutines.*
import test.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = ordinary("O", IC("K"))
    if (res != "OK") return "FAIL 1: $res"
    builder {
        res = suspend("O", IC("K"))
    }
    return res
}
