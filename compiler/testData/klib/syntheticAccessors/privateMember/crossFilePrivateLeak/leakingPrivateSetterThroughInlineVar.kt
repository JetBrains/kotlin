// JVM_ABI_K1_K2_DIFF
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
    if (result != 1) return result.toString()
    return "OK"
}
