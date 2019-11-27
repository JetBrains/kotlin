// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// CHECK_CASES_COUNT: function=foo count=3
// CHECK_IF_COUNT: function=foo count=0

import kotlin.test.assertEquals

fun foo(x : String) : String {
    when (x) {
        "abc" -> return "abc"
        "efg", "ghi", "abc" -> return "efg_ghi"
        else -> return "other"
    }
}

fun box() : String {
    assertEquals("abc", foo("abc"))
    assertEquals("efg_ghi", foo("efg"))
    assertEquals("efg_ghi", foo("ghi"))

    assertEquals("other", foo("xyz"))

    return "OK"
}
