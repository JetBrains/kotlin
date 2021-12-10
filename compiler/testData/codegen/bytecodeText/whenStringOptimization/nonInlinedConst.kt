// !LANGUAGE: -InlineConstVals
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature

const val y = "cde"

fun foo(x : String) : String {
    when (x) {
        "abc", "${y}" -> return "abc_cde"
        "e" + "fg", "ghi" -> return "efg_ghi"
    }

    return "other"
}

// 0 LOOKUPSWITCH
