// !LANGUAGE: -ReleaseCoroutines
// WITH_COROUTINES
// WITH_REFLECT
// DONT_TARGET_EXACT_BACKEND: JS_IR

// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS

import helpers.*
import kotlin.coroutines.experimental.*

class A {
    fun noArgs() = "OK"
}

fun box(): String {
    if (A::noArgs.isSuspend) return "FAIL"
    return "OK"
}
