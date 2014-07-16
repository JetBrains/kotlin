fun foo(x : String) : String {
    val y = "cde"
    when (x) {
        "abc", "${y}" -> return "abc_cde"
        "e" + "fg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

// 1 LOOKUPSWITCH
