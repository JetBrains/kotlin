// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
