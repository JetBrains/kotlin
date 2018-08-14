// WITH_RUNTIME
// CHECK_CASES_COUNT: function=foo count=4
// CHECK_IF_COUNT: function=foo count=0

import kotlin.test.assertEquals

fun foo(x : String) : String {
    return when (x) {
        "abc", "cde" -> "abc_cde"
        "efg", "ghi" -> "efg_ghi"
        else -> "other"
    }
}

fun box() : String {
    assertEquals("abc_cde", foo("abc"))
    assertEquals("abc_cde", foo("cde"))
    assertEquals("efg_ghi", foo("efg"))
    assertEquals("efg_ghi", foo("ghi"))

    assertEquals("other", foo("xyz"))

    return "OK"
}
