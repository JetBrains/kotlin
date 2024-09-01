// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class A {
    public inline fun publicInlineMethod(crossinline f: () -> String) = object {
        private fun privateMethod() = "O"
        inline fun run() = privateMethod() + f()
    }.run()
}

fun box(): String {
    return A().publicInlineMethod { "K" }
}
