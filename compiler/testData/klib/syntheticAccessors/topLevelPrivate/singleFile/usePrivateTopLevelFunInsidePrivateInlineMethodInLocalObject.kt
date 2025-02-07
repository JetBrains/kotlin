// KT-72840: java.lang.NoSuchFieldError: $f
// IGNORE_INLINER: BYTECODE
// IGNORE_BACKEND: ANDROID
// IGNORE_LIGHT_ANALYSIS
internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    private inline fun impl() = privateMethod() + f()
    public fun run() = impl()
}.run()

private fun privateMethod() = "O"

fun box(): String {
    return internalInlineMethod { "K" }
}
