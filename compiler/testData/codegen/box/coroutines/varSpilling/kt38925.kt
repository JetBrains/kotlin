// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
