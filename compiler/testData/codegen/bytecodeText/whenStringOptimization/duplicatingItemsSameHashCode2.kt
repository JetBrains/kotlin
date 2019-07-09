// IGNORE_BACKEND: JVM
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

// Expecting 0 LOOKUPSWITCH as there is only 2 different hash codes.
// An IF casecade is more efficient.
// 0 LOOKUPSWITCH
