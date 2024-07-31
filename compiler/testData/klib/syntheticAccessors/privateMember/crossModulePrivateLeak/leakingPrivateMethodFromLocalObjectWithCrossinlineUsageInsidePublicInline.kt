// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "O"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineMethod(crossinline f: () -> String) = object {
        fun run() = privateMethod() + f()
    }.run()
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A().publicInlineMethod { "K" }
}
