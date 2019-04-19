// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun map(): Map<String, String?> = mapOf("x" to null)
val x: String by map()

fun box(): String {
    try {
        x
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should not have been thrown"
}
