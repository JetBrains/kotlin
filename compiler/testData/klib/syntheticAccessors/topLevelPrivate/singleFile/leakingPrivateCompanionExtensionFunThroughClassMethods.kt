// LANGUAGE: +CompanionBlocksAndExtensions
class A {
    companion {
        internal inline fun internalInlineStaticMethod() = o()
    }

    internal inline fun internalInlineMethod() = k()
}

private companion fun A.o() = "O"
private companion fun A.k() = "K"

fun box(): String {
    return A.internalInlineStaticMethod() + A().internalInlineMethod()
}
