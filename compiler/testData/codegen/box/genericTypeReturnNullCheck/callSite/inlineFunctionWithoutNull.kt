// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

inline fun <T> foo(): T = "" as T

fun box(): String {
    foo<String>()
    return "OK"
}
