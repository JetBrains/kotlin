// LANGUAGE_VERSION: 1.2
// WITH_COROUTINES
// WITH_REFLECT

// IGNORE_BACKEND: JS_IR
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
