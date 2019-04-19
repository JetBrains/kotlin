// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> bar(): () -> T = { null as T }

fun box(): String {
    try {
        val x = bar<String>()
        val y = x()
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}
