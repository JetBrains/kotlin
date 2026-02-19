class A {
    private fun privateMethod() = "OK"

    internal inline fun internalInlineMethod() = privateMethod()
}

fun box(): String {
    return A().internalInlineMethod()
}
