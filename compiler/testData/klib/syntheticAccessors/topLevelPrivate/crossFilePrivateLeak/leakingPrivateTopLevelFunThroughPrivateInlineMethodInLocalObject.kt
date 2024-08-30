// IGNORE_BACKEND: ANY
// ^^^ Muted because IR validation fails due to presence of a link to `privateMethod()` inside the body of `inline fun impl()`
//     in anonymous object that was copied to the call site in another file. To be fixed in KT-71078.

// FILE: A.kt
internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    private inline fun impl() = privateMethod() + f()
    public fun run() = impl()
}.run()

private fun privateMethod() = "O"

// FILE: main.kt
fun box(): String {
    return internalInlineMethod { "K" }
}
