// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
class A {
    companion object {
        private fun privateMethod() = "OK"
    }

    inner class Inner {
        internal inline fun internalMethod() = this@A.privateMethod()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalMethod()
}
