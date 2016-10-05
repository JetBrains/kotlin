// WITH_RUNTIME

import kotlin.test.assertEquals

fun String.rebuild(): String {
    var result = ""
    for (c in this) {
        result += c
    }
    return result
}

fun box(): String {
    assertEquals("1234", "1234".rebuild())
    return "OK"
}
