// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND: JVM_IR

const val y = "cde"

fun foo(x : String) : String {
    when (x) {
        "abc", "${y}" -> return "abc_cde"
        "e" + "fg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

// 0 LOOKUPSWITCH
