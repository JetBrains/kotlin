class A {
    private var privateVar = 12

    internal inline var inlineVar: Int
        get() = privateVar
        set(value) {
            privateVar = value
        }
}

fun box(): String {
    var result = 0
    A().run {
        result += inlineVar
        inlineVar = 3
        result += inlineVar
    }
    if (result != 15) return result.toString()
    return "OK"
}
