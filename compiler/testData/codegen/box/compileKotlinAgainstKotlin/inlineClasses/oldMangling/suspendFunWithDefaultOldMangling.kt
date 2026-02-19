// TARGET_BACKEND: JVM
// LANGUAGE: +InlineClasses
// WITH_STDLIB
// MODULE: lib
// USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
// FILE: a.kt

package a

var result = ""

inline class P(val value: String)

suspend fun foo(p: P = P("OK")) {
    result = p.value
}

// MODULE: main(lib)
// WITH_COROUTINES
// FILE: b.kt

import kotlin.coroutines.*
import helpers.*
import a.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        foo()
    }
    return result
}
