// !API_VERSION: LATEST
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun box(): String {
    val s: String? = null
    try {
        s!!
        return "Fail: NPE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != NullPointerException::class) return "Fail: exception class should be NPE: ${e::class}"
        return "OK"
    }
}
