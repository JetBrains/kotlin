// LANGUAGE: +CompanionBlocksAndExtensions
class A {
    companion {
        private fun o() = "O"
        private fun k() = "K"

        internal inline fun internalInlineStaticMethod() = o()
    }

    internal inline fun internalInlineMethod() = k()
}

fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
