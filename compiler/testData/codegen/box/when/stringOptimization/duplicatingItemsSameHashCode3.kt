// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

fun foo(x : String) : String {
    assert("abz]".hashCode() == "aby|".hashCode())

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
