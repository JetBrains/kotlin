import kotlin.test.assertEquals

fun foo(x : String) : String {
    assert("abz]".hashCode() == "aby|".hashCode())

    when (x) {
        "abz]", "cde" -> return "abz_cde"
        "aby|", "ghi", "abz]" -> return "aby_ghi"
    }

    return "other"
}

fun box() : String {
    assertEquals("abz_cde", foo("abz]"))
    assertEquals("abz_cde", foo("cde"))
    assertEquals("aby_ghi", foo("aby|"))
    assertEquals("aby_ghi", foo("ghi"))

    assertEquals("other", foo("xyz"))

    return "OK"
}
