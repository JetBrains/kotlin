const val y = "cde"

fun foo(x : String) : String {
    when (x) {
        "abc", "${y}" -> return "abc_cde"
        "e" + "fg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

// 1 LOOKUPSWITCH
