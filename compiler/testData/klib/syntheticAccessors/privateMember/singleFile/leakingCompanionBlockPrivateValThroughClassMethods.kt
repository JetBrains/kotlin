// LANGUAGE: +CompanionBlocksAndExtensions
class A {
    companion {
        private val o = "O"
        private val k = "K"

        internal inline fun internalInlineStaticMethod() = o
    }

    internal inline fun internalInlineMethod() = k
}

fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
