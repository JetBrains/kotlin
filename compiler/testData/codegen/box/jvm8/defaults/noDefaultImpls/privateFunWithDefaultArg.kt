// CHECK_BYTECODE_LISTING
// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_RUNTIME
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface Foo {

    fun bar(): String {
        var result = "fail"
        builder {
            result = fooSuspend()
        }
        return foo() + result

    }

    private fun foo(s: String = "O"): String {
        return s
    }

    private suspend fun fooSuspend(s: String = "K"): String {
        return s
    }
}

fun box(): String {
    return object : Foo {}.bar()
}