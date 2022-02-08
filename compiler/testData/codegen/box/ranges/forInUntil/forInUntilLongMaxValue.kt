// WITH_STDLIB

import kotlin.test.assertEquals

fun box(): String {
    for (i in Long.MAX_VALUE until Long.MAX_VALUE) {
        throw AssertionError("This loop shoud not be executed")
    }
    return "OK"
}
