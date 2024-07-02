import A

// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: a.kt
private var _privateVar = 21
private var Int.privateVar: Int
    get() = _privateVar + this
    set(value) {
        _privateVar = value
    }

internal inline fun customSetVar(value: Int) {
    0.privateVar = value
}

internal inline fun customGetVar(): Int = 0.privateVar

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
