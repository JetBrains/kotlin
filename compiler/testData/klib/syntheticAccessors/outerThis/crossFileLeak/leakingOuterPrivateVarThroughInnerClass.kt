// FILE: Outer.kt
class Outer {
    private var privateVar = 20

    inner class Inner {
        internal inline fun customVarGetter() = privateVar
        internal inline fun customVarSetter(value: Int) {
            privateVar = value
        }
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0
    val inner = Outer().Inner()

    result += inner.customVarGetter()
    inner.customVarSetter(22)
    result += inner.customVarGetter()
    if (result != 42) return result.toString()
    return "OK"
}
