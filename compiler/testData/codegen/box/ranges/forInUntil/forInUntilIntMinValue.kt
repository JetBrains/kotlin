// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    for (i in 0 until Int.MIN_VALUE) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
