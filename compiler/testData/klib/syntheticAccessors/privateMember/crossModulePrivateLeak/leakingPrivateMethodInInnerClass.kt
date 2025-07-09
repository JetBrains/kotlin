// MODULE: lib
// FILE: A.kt
class A {
    inner class Inner{
        private fun privateMethod() = "OK"

        internal inline fun internalInlineMethod() = privateMethod()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().Inner().internalInlineMethod()
}
