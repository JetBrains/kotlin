// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 12

    private var privateInlineVar1: Int
        inline get() = privateVar
        inline set(value) {
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
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += inlineVar
        inlineVar = 10
        result += inlineVar
    }
    if (result != 22) return result.toString()
    return "OK"
}
