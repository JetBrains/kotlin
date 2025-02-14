// IGNORE_INLINER: IR
// ^ outer this accesors are not generated correctly in jvm with ir inliner
// FILE: Outer.kt

class Outer {
    companion object {
        private fun privateMethod() = "OK"
    }

    inner class Inner {
        internal inline fun internalMethod() = privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalMethod()
}
