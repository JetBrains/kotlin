// LANGUAGE: +CompanionBlocksAndExtensions
class Outer {
    companion {
        private val o = "O"
        private val k = "K"
    }

    inner class Inner {
        internal inline fun internalInlineMethod() = o
    }

    class Nested {
        internal inline fun internalInlineMethod() = k
    }
}

fun box(): String {
    return Outer().Inner().internalInlineMethod() + Outer.Nested().internalInlineMethod()
}
