// WITH_RUNTIME
// CHECK_CASES_COUNT: function=foo1 count=4
// CHECK_IF_COUNT: function=foo1 count=0
// CHECK_CASES_COUNT: function=foo2 count=4
// CHECK_IF_COUNT: function=foo2 count=0

import kotlin.test.assertEquals

fun foo1(x : String) : String {
    when (x) {
        "abc", "cde" -> return "abc_cde"
        "efg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

fun foo2(x : String) : String {
    when (x) {
        "abc", "cde" -> return "abc_cde"
        "efg", "ghi" -> return "efg_ghi"
        else -> return "other"
    }
}

fun box() : String {
    //foo1
    assertEquals("abc_cde", foo1("abc"))
    assertEquals("abc_cde", foo1("cde"))
    assertEquals("efg_ghi", foo1("efg"))
    assertEquals("efg_ghi", foo1("ghi"))

    assertEquals("other", foo1("xyz"))

    //foo2
    assertEquals("abc_cde", foo2("abc"))
    assertEquals("abc_cde", foo2("cde"))
    assertEquals("efg_ghi", foo2("efg"))
    assertEquals("efg_ghi", foo2("ghi"))

    assertEquals("other", foo2("xyz"))

    return "OK"
}
