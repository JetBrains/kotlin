// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun foo() {
    bar {
        val p = false
        baz(p, "".ifEmpty { "OK" })
    }
}

var res = "FAIL"

fun bar(f: suspend () -> Unit) {
    f.startCoroutine(EmptyContinuation)
}

fun baz(p: Boolean, s: String?) {
    res = s!!
}

fun box(): String {
    foo()
    return res
}
