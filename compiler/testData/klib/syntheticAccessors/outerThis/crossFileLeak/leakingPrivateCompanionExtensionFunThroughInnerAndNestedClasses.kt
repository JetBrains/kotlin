// LANGUAGE: +CompanionBlocksAndExtensions
// FILE: Outer.kt
class Outer {
    inner class Inner {
        internal inline fun internalInlineMethod() = o()
    }

    class Nested {
        internal inline fun internalInlineMethod() = k()
    }
}

private companion fun Outer.o() = "O"
private companion fun Outer.k() = "K"

// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalInlineMethod() + Outer.Nested().internalInlineMethod()
}
