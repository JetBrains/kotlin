class Outer {
    private fun privateMethod() = "OK"
    class Nested{
        internal inline fun internalInlineMethod() = Outer().privateMethod()
    }
}

fun box(): String {
    return Outer.Nested().internalInlineMethod()
}
