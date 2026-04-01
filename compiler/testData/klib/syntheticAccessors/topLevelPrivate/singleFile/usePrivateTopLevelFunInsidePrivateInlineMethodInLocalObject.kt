// KT-72840: java.lang.NoSuchFieldError: $f
// IGNORE_BACKEND: JVM_IR

internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    private inline fun impl() = privateMethod() + f()
    public fun run() = impl()
}.run()

private fun privateMethod() = "O"

fun box(): String {
    return internalInlineMethod { "K" }
}
