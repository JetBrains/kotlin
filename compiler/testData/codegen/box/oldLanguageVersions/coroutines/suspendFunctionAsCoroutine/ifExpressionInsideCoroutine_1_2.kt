// !LANGUAGE: -ReleaseCoroutines
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS
// WITH_RUNTIME
// WITH_COROUTINES
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6

import helpers.*
import kotlin.coroutines.experimental.*

val f = run {
    buildSequence {
        if (true) {
            yield("OK")
        }
    }.toList()
}

fun box(): String {
    return f[0]
}
