// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// MODULE: lib
// FILE: a.kt
private var privateVar = 12

internal inline var inlineVar: Int
    get() = privateVar
    set(value) {
        privateVar = value
    }

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += inlineVar
    inlineVar = 3
    result += inlineVar
    if (result != 15) return result.toString()
    return "OK"
}
