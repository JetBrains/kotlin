// IGNORE_BACKEND: NATIVE

// MODULE: lib
// FILE: A.kt
class A {
    internal inline fun customSetVar(value: Int) {
        privateVar = value
    }

    internal inline fun customGetVar(): Int = privateVar
}

private var _privateVar = 12
private var A.privateVar: Int
    get() = _privateVar
    set(value) {
        _privateVar = value
    }

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += customGetVar()
        customSetVar(1)
        result += customGetVar()
    }
    if (result != 13) return result.toString()
    return "OK"
}
