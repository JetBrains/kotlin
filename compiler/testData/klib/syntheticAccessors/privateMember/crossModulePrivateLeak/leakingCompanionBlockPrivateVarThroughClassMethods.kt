// LANGUAGE: +CompanionBlocks +CompanionExtensions
// MODULE: lib
// FILE: A.kt
class A {
    companion {
        private var o = ""
        private var k = ""

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

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    A.internalInlineStaticSetter("O")
    A().internalInlineSetter("K")
    return A.internalInlineStaticGetter() + A().internalInlineGetter()
}
