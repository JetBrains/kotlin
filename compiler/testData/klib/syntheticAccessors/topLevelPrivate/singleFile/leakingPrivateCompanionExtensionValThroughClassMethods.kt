// LANGUAGE: +CompanionBlocks +CompanionExtensions
class A {
    companion {
        internal inline fun internalInlineStaticMethod() = o
    }

    internal inline fun internalInlineMethod() = k
}

private companion val A.o = "O"
private companion val A.k = "K"

fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
