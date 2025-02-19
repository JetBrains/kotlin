// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype, +ContextParameters
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_BACKEND_K1: JVM_IR, WASM, NATIVE
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend context(String) () -> Unit) {
    c.startCoroutine("OK", EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"
var test3 = "failed"

class A: context(String) () -> Unit {
    override fun invoke(p1: String) {
        test1 = p1
    }
}

class B: String.() -> Unit {
    override fun invoke(p1: String) {
        test2 = p1
    }
}

class C: (String) -> Unit {
    override fun invoke(p1: String) {
        test3 = p1
    }
}

fun box(): String {
    runSuspend((::A)())
    runSuspend((::B)())
    runSuspend((::C)())
    return if(test1 == "OK" && test2 == "OK" && test3 == "OK") "OK" else "fail"
}
