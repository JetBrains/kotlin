// !LANGUAGE: +GenerateNullChecksForGenericTypeReturningFunctions
// TARGET_BACKEND: JVM
// WITH_RUNTIME

fun <T> foo(): T = null as T

fun <T> test(x: T) {
    var y = x!!
    y = foo()
}

fun box(): String {
    try {
        test<String?>("")
    } catch (e: KotlinNullPointerException) {
        return "Fail: KotlinNullPointerException should not have been thrown"
    }
    return "OK"
}
