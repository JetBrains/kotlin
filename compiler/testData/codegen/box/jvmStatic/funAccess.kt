// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

var holder = ""

fun getA(): A {
    holder += "OK"
    return A
}

object A {
    @JvmStatic fun a(): String {
        return holder
    }
}

fun box(): String {
    return getA().a()
}
