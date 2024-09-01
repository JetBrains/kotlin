class A {
    private operator fun plus(increment: Int): String = "OK"

    internal inline fun internalInlineMethod() = this + 1
}

fun box(): String {
    return A().internalInlineMethod()
}
