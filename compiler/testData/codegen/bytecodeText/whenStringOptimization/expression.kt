fun foo(x : String) : String {
    return when (x) {
        "abc", "cde" -> "abc_cde"
        "efg", "ghi" -> "efg_ghi"
        else -> "other"
    }
}

// 1 LOOKUPSWITCH
