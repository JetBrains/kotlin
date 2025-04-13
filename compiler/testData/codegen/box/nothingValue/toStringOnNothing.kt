// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ISSUE: KT-72746

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
