// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

inline fun <T> foo(): T = "" as T

fun box(): String {
    foo<String>()
    return "OK"
}
