// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> foo(): T = null as T

fun box(): String {
    try {
        val x: String? = foo()
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
