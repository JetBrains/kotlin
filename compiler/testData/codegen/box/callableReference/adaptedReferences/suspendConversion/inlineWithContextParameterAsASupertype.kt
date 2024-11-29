// LANGUAGE: +FunctionalTypeWithExtensionAsSupertype, +ContextParameters
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// IGNORE_BACKEND_K1: JVM, JVM_IR, WASM, NATIVE
import helpers.*
import kotlin.coroutines.*

fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var test1 = "failed"
var test2 = "failed"
var test3 = "failed"

class A: String.() -> Unit {
    override fun invoke(p1: String) {
        test1 = p1
    }
}

class B: (String) -> Unit {
    override fun invoke(p1: String) {
        test2 = p1
    }
}

class C: context(String)() -> Unit {
    override fun invoke(p1: String) {
        test3 = p1
    }
}

suspend inline fun invokeSuspend(fn: suspend context(String) () -> Unit) { fn("OK") }

fun box(): String {
    runSuspend {
        invokeSuspend((::A)())
        invokeSuspend((::B)())
        invokeSuspend((::C)())
    }
    return if(test1 == "OK" && test2 == "OK" && test3 == "OK") "OK" else "fail"
}