// LANGUAGE: +CompanionBlocks +CompanionExtensions
class A {
    companion {
        private fun privateMethod() = "OK"
    }
}

@Suppress("INVISIBLE_REFERENCE")
internal inline fun internalInlineMethod() = A.privateMethod()

fun box(): String {
    return internalInlineMethod()
}
