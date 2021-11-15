// NO_UNIFIED_NULL_CHECKS
// WITH_STDLIB
// TARGET_BACKEND: JVM

fun box(): String {
    val s: String? = null
    try {
        s!!
        return "Fail: KNPE should have been thrown"
    } catch (e: Throwable) {
        if (e::class != KotlinNullPointerException::class) return "Fail: exception class should be KNPE: ${e::class}"
        return "OK"
    }
}
