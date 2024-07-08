// MODULE: lib
// FILE: Outer.kt
class Outer {
    companion object {
        private fun privateMethod() = "OK"
    }

    inner class Inner {
        internal inline fun internalMethod() = privateMethod()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalMethod()
}
