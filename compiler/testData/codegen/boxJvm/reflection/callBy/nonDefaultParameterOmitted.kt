// TARGET_BACKEND: JVM
// WITH_REFLECT

fun foo(x: Int, y: Int = 2) = x + y

fun box(): String {
    try {
        ::foo.callBy(mapOf())
        return "Fail: IllegalArgumentException must have been thrown"
    }
    catch (e: IllegalArgumentException) {
        // OK
    }

    try {
        ::foo.callBy(mapOf(::foo.parameters.last() to 1))
        return "Fail: IllegalArgumentException must have been thrown"
    }
    catch (e: IllegalArgumentException) {
        // OK
    }

    return "OK"
}
