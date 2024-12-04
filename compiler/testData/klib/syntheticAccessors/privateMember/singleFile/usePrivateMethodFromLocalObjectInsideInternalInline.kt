class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = object {
        fun run() = privateMethod()
    }.run()
}

fun box(): String {
    return A().internalInlineMethod()
}
