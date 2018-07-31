// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

object A {

    @JvmStatic fun test(b: String = "OK") : String {
        return b
    }
}

fun box(): String {

    if (A.test() != "OK") return "fail 1"

    return "OK"
}
