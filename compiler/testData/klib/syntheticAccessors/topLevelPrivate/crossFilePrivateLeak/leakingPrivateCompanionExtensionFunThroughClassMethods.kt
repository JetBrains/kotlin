// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: A.kt
class A {
    companion {
        internal inline fun internalInlineStaticMethod() = o()
    }

    internal inline fun internalInlineMethod() = k()
}

private companion fun A.o() = "O"
private companion fun A.k() = "K"

// FILE: main.kt
fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
