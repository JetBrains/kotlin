// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> map(): Map<String, T?> = mapOf("x" to null)
val x: String by map<String>()

fun box(): String {
    try {
        x
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should not have been thrown"
}
