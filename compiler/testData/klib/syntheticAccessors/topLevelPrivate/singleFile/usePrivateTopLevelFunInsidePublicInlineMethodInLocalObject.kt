// KT-72840: java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: ANDROID

internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
    public inline fun run() = privateMethod() + f()
}.run()

private fun privateMethod() = "O"

fun box(): String {
    return internalInlineMethod { "K" }
}
