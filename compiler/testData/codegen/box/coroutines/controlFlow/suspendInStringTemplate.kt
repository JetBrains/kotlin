// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// IGNORE_BACKEND: JS
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

// KT-36897

suspend fun foo(str: String) = str

suspend fun test(): String {
    return foo("""${foo("O")}${foo("K")}""")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "FAIL"

    builder {
        result = test()
    }

    return result
}