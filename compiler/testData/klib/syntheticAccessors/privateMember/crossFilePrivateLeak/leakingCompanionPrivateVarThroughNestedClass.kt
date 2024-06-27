// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    companion object{
        private var privateVar = 20
    }

    class Nested {
        internal inline fun customVarGetter() = privateVar
        internal inline fun customVarSetter(value: Int) {
            privateVar = value
        }
    }
}

// FILE: main.kt
fun box(): String {
    var result = 0
    val nested = A.Nested()

    result += nested.customVarGetter()
    nested.customVarSetter(22)
    result += nested.customVarGetter()
    if (result != 42) return result.toString()
    return "OK"
}
