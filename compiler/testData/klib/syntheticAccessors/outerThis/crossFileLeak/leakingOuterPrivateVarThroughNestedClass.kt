// FILE: Outer.kt
class Outer {
    private var privateVar = 20

    class Nested {
        internal inline fun customVarGetter(outer: Outer) = outer.privateVar
        internal inline fun customVarSetter(outer: Outer, value: Int) {
            outer.privateVar = value
        }
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0
    val outer = Outer()
    val nested = Outer.Nested()

    result += nested.customVarGetter(outer)
    nested.customVarSetter(outer, 22)
    result += nested.customVarGetter(outer)
    if (result != 42) return result.toString()
    return "OK"
}
