// FILE: a.kt
private var privateVar = 12

private inline var privateInlineVar1: Int
    get() = privateVar
    set(value) {
        privateVar = value
    }

private inline var privateInlineVar2: Int
    get() = privateInlineVar1
    set(value) {
        privateInlineVar1 = value
    }

internal inline var inlineVar: Int
    get() = privateInlineVar2
    set(value) {
        privateInlineVar2 = value
    }

// FILE: main.kt
fun box(): String {
    var result = 0
    result += inlineVar
    inlineVar = 3
    result += inlineVar
    if (result != 15) return result.toString()
    return "OK"
}
