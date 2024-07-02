// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
class A {
    private var privateVar = 20

    inner class Inner {
        internal inline fun customVarGetter() = privateVar
        internal inline fun customVarSetter(value: Int) {
            privateVar = value
        }
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    val inner = A().Inner()

    result += inner.customVarGetter()
    inner.customVarSetter(22)
    result += inner.customVarGetter()
    if (result != 42) return result.toString()
    return "OK"
}
