// FILE: Outer.kt
class Outer {
    private fun privateMethod() = "OK"
    inner class Inner{
        internal inline fun internalMethod() = privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalMethod()
}
