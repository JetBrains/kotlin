// KT-72840: java.lang.AssertionError: AFTER mandatory stack transformations: incorrect bytecode
// IGNORE_INLINER: BYTECODE

class A {
    public inline fun publicInlineMethod(crossinline f: () -> String) = object {
        private fun privateMethod() = "O"
        inline fun run() = privateMethod() + f()
    }.run()
}

fun box(): String {
    return A().publicInlineMethod { "K" }
}
