// ISSUE: KT-72746

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0 2.1.0
// ^^^ KT-72746 fixed in 2.1.20-Beta1

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
