// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: A.kt
class A {
    companion {
        internal inline fun internalInlineStaticGetter() = o
        internal inline fun internalInlineStaticSetter(value: String) {
            o = value
        }
    }

    internal inline fun internalInlineGetter() = k
    internal inline fun internalInlineSetter(value: String) {
        k = value
    }
}

private companion var A.o = ""
private companion var A.k = ""

// FILE: main.kt
fun box(): String {
    A.internalInlineStaticSetter("O")
    A().internalInlineSetter("K")
    return A.internalInlineStaticGetter() + A().internalInlineGetter()
}
