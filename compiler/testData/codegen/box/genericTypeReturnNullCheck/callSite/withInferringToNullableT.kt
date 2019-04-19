// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

fun <T> foo(): T = null as T
fun <T> test() {
    val x: T? = foo()
}

fun box(): String {
    test<String>()
    return "OK"
}
