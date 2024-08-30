// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: A.kt
internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    public inline fun run() = privateMethod() + f()
}.run()

private fun privateMethod() = "O"

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineMethod { "K" }
}
