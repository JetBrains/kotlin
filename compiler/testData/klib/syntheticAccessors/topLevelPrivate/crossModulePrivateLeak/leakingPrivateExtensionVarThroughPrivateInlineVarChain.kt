// MODULE: lib
// FILE: a.kt
private var _privateVar = 21
private var Int.privateVar: Int
    get() = _privateVar + this
    set(value) {
        _privateVar = value
    }

private var Int.privateInlineVar1: Int
    inline get() = privateVar
    inline set(value) {
        privateVar = value
    }

private inline var Int.privateInlineVar2: Int
    get() = privateInlineVar1
    set(value) {
        privateInlineVar1 = value
    }

internal inline var Int.inlineVar: Int
    get() = privateInlineVar2
    set(value) {
        privateInlineVar2 = value
    }

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += 0.inlineVar
    0.inlineVar = 10
    result += 0.inlineVar
    if (result != 22) return result.toString()
    return "OK"
}
