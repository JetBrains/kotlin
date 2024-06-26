// FILE: A.kt
class A

// FILE: B.kt
private var _privateVar = 12
private var A.privateVar: Int
    get() = _privateVar
    set(value) {
        _privateVar = value
    }

internal inline fun A.customSetVar(value: Int) {
    privateVar = value
}

internal inline fun A.customGetVar(): Int = privateVar

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
