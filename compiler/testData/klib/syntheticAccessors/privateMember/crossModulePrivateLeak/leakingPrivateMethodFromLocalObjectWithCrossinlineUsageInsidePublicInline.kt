// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
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
