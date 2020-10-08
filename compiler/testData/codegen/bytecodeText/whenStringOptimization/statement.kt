// IGNORE_BACKEND_FIR: JVM_IR
fun foo1(x : String) : String {
    when (x) {
        "abc", "cde" -> return "abc_cde"
        "efg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

fun foo2(x : String) : String {
    when (x) {
        "abc", "cde" -> return "abc_cde"
        "efg", "ghi" -> return "efg_ghi"
        else -> return "other"
    }
}

// 2 LOOKUPSWITCH

