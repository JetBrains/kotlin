// FILE: A.kt
class A {
    companion object{
        private var privateVar = 12
    }

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
        result += inlineVar
        inlineVar = 1
        result += inlineVar
    }
    if (result != 13) return result.toString()
    return "OK"
}
