import kotlin.test.assertEquals

fun foo(x : String) : String {
    assert("abz]".hashCode() == "aby|".hashCode())

    when (x) {
        "abz]" -> return "abz"
        "ghi"  -> return "ghi"
        "aby|" -> return "aby"
        "abz]" -> return "fail"
    }

    return "other"
}

fun box() : String {
    assertEquals("abz", foo("abz]"))
    assertEquals("aby", foo("aby|"))
    assertEquals("ghi", foo("ghi"))

    assertEquals("other", foo("xyz"))

    return "OK"
}
