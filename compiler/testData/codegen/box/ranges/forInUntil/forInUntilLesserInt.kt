// WITH_RUNTIME

import kotlin.test.assertEquals

fun box(): String {
    for (i in 10 until 0) {
        throw AssertionError("This loop should not be executed")
    }
    return "OK"
}