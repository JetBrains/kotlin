// FILE: A.kt
class A {
    private var privateVar = 12

    internal inline fun customSetVar(value: Int) {
        privateVar = value
    }

    internal inline fun customGetVar(): Int = privateVar
}

// FILE: main.kt
fun box(): String {
    var result = 0
    A().run {
        result += customGetVar()
        customSetVar(1)
        result += customGetVar()
    }
    if (result != 13) return result.toString()
    return "OK"
}
