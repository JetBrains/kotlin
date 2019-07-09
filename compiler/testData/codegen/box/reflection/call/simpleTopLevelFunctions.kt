// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

fun String.foo(): Int = length

var state = "Fail"

fun bar(result: String) {
    state = result
}

fun box(): String {
    val f = (String::foo).call("abc")
    if (f != 3) return "Fail: $f"

    try {
        (String::foo).call()
        return "Fail: IllegalArgumentException should have been thrown"
    }
    catch (e: IllegalArgumentException) {}

    try {
        (String::foo).call(42)
        return "Fail: IllegalArgumentException should have been thrown"
    }
    catch (e: IllegalArgumentException) {}

    (::bar).call("OK")
    return state
}
