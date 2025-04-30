// IGNORE_NATIVE: compatibilityTestMode=OldArtifactNewCompiler
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
