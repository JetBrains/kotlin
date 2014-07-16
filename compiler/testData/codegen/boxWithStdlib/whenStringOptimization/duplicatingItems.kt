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

