// FILE: A.kt
class A {
    var privateVar = 12
        private set

    internal inline var inlineVar: Int
        get() = privateVar
        set(value) {
            privateVar = value
        }
}

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
