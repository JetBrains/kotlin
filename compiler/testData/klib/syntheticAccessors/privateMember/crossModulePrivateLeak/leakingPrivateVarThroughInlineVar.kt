// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 12

    internal inline var inlineVar: Int
        get() = privateVar
        set(value) {
            privateVar = value
        }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        inlineVar += result
        inlineVar = 1
        result += inlineVar
    }
    if (result != 13) return result.toString()
    return "OK"
}
