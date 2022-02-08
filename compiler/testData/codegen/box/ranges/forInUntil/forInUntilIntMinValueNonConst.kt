// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    var bound = Int.MIN_VALUE
    for (i in 0 until bound) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
