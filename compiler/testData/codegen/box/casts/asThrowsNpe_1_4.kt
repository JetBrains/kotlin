// !API_VERSION: LATEST
// TARGET_BACKEND: JVM

fun box(): String {
    val s: String? = null
    try {
        s as String
        return "Fail: NPE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != NullPointerException::class) return "Fail: exception class should be NPE: ${e::class}"
        return "OK"
    }
}
