// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: lib
// FILE: A.kt
class A {
    companion {
        private fun privateMethod() = "OK"
    }
}

@Suppress("INVISIBLE_REFERENCE")
internal inline fun internalInlineMethod() = A.privateMethod()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineMethod()
}
