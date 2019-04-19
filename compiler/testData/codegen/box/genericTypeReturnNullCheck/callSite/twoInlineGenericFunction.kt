// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

inline fun <T> foo(): T = null as T
inline fun <T> bar(): T = foo<T>()

fun box(): String {
    try {
        bar<String>()
    } catch (e: KotlinNullPointerException) {
        return "OK"
    }
    return "Fail: KotlinNullPointerException should have been thrown"
}
