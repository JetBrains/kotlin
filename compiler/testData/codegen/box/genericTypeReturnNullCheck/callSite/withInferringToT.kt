// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> foo(): T = null as T

fun <T> bar() {
    val x: T = foo()
}

fun box(): String {
    bar<String?>()
    return "OK"
}
