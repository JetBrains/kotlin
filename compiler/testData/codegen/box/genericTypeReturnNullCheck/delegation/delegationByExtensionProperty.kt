// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

val String.map get() = mapOf<String, String?>("x" to null)
val x: String by "".map

fun box(): String {
    try {
        x
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should not have been thrown"
}
