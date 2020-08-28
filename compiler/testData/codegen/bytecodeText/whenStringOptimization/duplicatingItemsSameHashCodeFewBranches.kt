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

// JVM_TEMPLATES:
// 1 LOOKUPSWITCH

// JVM_IR_TEMPLATES:
// Expecting 0 LOOKUPSWITCH as there is only 2 different hash codes. An IF cascade is more efficient.
// 0 LOOKUPSWITCH
