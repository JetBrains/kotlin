// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

inline fun <T> foo(): T = "" as T

fun box(): String {
    try {
        foo<String>()
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
