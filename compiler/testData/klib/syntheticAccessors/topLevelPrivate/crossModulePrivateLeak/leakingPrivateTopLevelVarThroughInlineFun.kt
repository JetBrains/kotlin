// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: a.kt
private var privateVar = 12

internal inline fun customSetVar(value: Int) {
    privateVar = value
}

internal inline fun customGetVar(): Int = privateVar

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += customGetVar()
    customSetVar(3)
    result += customGetVar()
    if (result != 15) return result.toString()
    return "OK"
}
