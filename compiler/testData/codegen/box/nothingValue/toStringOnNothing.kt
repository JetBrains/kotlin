// ISSUE: KT-72746
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ KT-72746: Compiler v2.1.10 has a bug in 1st compilation phase

fun box(): String {
    var test: Int? = null
    return if (test != null) {
        "Fail"
    } else {
        try {
            test!!.toString()
        } catch (_: NullPointerException) {
            "OK"
        }
    }
}
