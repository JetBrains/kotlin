// IGNORE_BACKEND: NATIVE

// WITH_STDLIB

import kotlin.test.assertEquals

fun foo(x : String) : String {
    assertEquals("abz]".hashCode(), "aby|".hashCode())

    when (x) {
        "abz]" -> return "abz"
        "ghi"  -> return "ghi"
        "aby|" -> return "aby"
        "abz]" -> return "fail"
        "uvw" -> return "uvw"
    }

    return "other"
}

fun box() : String {
    assertEquals("abz", foo("abz]"))
    assertEquals("aby", foo("aby|"))
    assertEquals("ghi", foo("ghi"))
    assertEquals("uvw", foo("uvw"))

    assertEquals("other", foo("xyz"))

    return "OK"
}
