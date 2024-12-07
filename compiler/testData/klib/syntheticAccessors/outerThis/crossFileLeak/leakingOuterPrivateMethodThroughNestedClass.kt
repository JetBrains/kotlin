// FILE: Outer.kt
class Outer {
    private fun privateMethod() = "OK"
    class Nested{
        internal inline fun internalInlineMethod() = Outer().privateMethod()
    }
}

// FILE: main.kt
fun box(): String {
    return Outer.Nested().internalInlineMethod()
}
