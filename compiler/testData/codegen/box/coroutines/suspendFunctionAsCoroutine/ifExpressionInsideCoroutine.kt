// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
