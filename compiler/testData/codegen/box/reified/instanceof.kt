// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_STDLIB

inline fun<reified T> isinstance(x: Any?): Boolean {
    return x is T
}

fun box(): String {
    assert(isinstance<String>("abc"))
    assert(isinstance<Int>(1))
    assert(!isinstance<Int>("abc"))

    return "OK"
}
