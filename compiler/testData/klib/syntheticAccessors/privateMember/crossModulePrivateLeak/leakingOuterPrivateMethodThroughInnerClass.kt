// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "OK"
    inner class Inner{
        internal inline fun internalMethod() = A().privateMethod()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().Inner().internalMethod()
}
