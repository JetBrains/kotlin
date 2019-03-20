// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    for (ch in (-10).toChar() until '\u0000') {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
