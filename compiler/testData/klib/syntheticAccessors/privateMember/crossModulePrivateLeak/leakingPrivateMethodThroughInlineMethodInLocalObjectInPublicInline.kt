// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: A.kt
class A {
    inline fun internalInlineMethod(crossinline f: () -> String) = object {
        private fun privateMethod() = "O"
        inline fun run() = privateMethod() + f()
    }.run()
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod { "K" }
}
