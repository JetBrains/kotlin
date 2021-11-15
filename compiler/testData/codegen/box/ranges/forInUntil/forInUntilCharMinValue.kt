// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    for (ch in (-10).toChar() until Char.MIN_VALUE) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
