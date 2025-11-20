// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val str1 = "Hello"
    val str2 = "Hello"
    if (!(str1 == str2))
        return "FAIL =="
    if (!(str1 === str2))
        return "FAIL ==="

    return "OK"
}
