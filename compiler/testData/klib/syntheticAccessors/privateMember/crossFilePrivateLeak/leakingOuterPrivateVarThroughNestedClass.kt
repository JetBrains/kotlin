// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    private var privateVar = 20

    class Nested {
        internal inline fun customVarGetter(outer: A) = outer.privateVar
        internal inline fun customVarSetter(outer: A, value: Int) {
            outer.privateVar = value
        }
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0
    val outer = A()
    val nested = A.Nested()

    result += nested.customVarGetter(outer)
    nested.customVarSetter(outer, 22)
    result += nested.customVarGetter(outer)
    if (result != 42) return result.toString()
    return "OK"
}
