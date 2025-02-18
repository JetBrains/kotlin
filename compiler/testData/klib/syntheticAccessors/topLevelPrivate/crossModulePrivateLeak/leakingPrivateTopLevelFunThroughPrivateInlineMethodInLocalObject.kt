// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// MODULE: lib
// KT-72840: java.lang.NoSuchFieldError: $f
// IGNORE_INLINER: BYTECODE
// FILE: A.kt
internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    private inline fun impl() = privateMethod() + f()
    public fun run() = impl()
}.run()

private fun privateMethod() = "O"

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineMethod { "K" }
}
