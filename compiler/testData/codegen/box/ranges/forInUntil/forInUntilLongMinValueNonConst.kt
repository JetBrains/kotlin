// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    var bound = Long.MIN_VALUE
    for (i in 0 until bound) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
