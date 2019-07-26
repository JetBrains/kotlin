// !API_VERSION: LATEST
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR

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
