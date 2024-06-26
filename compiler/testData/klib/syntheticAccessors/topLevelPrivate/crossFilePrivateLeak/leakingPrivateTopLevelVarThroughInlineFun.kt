// FILE: a.kt
private var privateVar = 12

internal inline fun customSetVar(value: Int) {
    privateVar = value
}

internal inline fun customGetVar(): Int = privateVar

// FILE: main.kt
fun box(): String {
    var result = 0
    result += customGetVar()
    customSetVar(3)
    result += customGetVar()
    if (result != 15) return result.toString()
    return "OK"
}
