// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// KT-72840: java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID
// IGNORE_LIGHT_ANALYSIS

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
