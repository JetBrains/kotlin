class A {
    private fun privateMethod() = "O"

    internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
        fun run() = privateMethod() + f()
    }.run()
}

fun box(): String {
    return A().internalInlineMethod { "K" }
}
