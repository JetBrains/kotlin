// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// IGNORE_BACKEND: NATIVE
// ^ KT-70583: Internal error in body lowering: java.lang.IllegalStateException: An attempt to generate an accessor after all accessors have been already added to their containers

// MODULE: lib
// FILE: A.kt
class A {
    public inline fun publicInlineMethod(crossinline f: () -> String) = object {
        private fun privateMethod() = "O"
        inline fun run() = privateMethod() + f()
    }.run()
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A().publicInlineMethod { "K" }
}
