// WITH_STDLIB
// CHECK_CASES_COUNT: function=foo count=3 TARGET_BACKENDS=JS
// CHECK_CASES_COUNT: function=foo count=4 IGNORED_BACKENDS=JS
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
