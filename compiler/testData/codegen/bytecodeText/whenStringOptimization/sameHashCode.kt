fun foo(x : String) : String {
    assert("abz]".hashCode() == "aby|".hashCode())

    when (x) {
        "abz]", "cde" -> return "abz_cde"
        "aby|", "ghi" -> return "aby_ghi"
    }

    return "other"
}

// 1 LOOKUPSWITCH
