// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    var bound = Char.MIN_VALUE
    for (ch in (-10).toChar() until bound) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
