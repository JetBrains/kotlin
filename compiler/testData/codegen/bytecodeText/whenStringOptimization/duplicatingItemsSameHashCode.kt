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

// 1 LOOKUPSWITCH
