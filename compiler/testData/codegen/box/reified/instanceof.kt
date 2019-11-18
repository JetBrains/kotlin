// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

inline fun<reified T> isinstance(x: Any?): Boolean {
    return x is T
}

fun box(): String {
    assert(isinstance<String>("abc"))
    assert(isinstance<Int>(1))
    assert(!isinstance<Int>("abc"))

    return "OK"
}
