// KT-72840: java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
// IGNORE_INLINER: BYTECODE
// IGNORE_BACKEND: ANDROID
// IGNORE_LIGHT_ANALYSIS
// FILE: A.kt
internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    public inline fun run() = privateMethod() + f()
}.run()

private fun privateMethod() = "O"

// FILE: main.kt
fun box(): String {
    return internalInlineMethod { "K" }
}
