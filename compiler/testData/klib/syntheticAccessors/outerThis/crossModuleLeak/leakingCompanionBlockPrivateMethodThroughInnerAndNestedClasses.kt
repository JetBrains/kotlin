// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: lib
// FILE: Outer.kt
class Outer {
    companion {
        private fun o() = "O"
        private fun k() = "K"
    }

    inner class Inner {
        internal inline fun internalInlineMethod() = o()
    }

    class Nested {
        internal inline fun internalInlineMethod() = k()
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return Outer().Inner().internalInlineMethod() + Outer.Nested().internalInlineMethod()
}
