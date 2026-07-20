// LANGUAGE: +CompanionBlocks +CompanionExtensions
// FILE: Outer.kt
class Outer {
    inner class Inner {
        internal inline fun internalInlineGetter() = o
        internal inline fun internalInlineSetter(value: String) {
            o = value
        }
    }

    class Nested {
        internal inline fun internalInlineGetter() = k
        internal inline fun internalInlineSetter(value: String) {
            k = value
        }
    }
}

private companion var Outer.o = ""
private companion var Outer.k = ""

// FILE: main.kt
fun box(): String {
    val inner = Outer().Inner()
    val nested = Outer.Nested()
    inner.internalInlineSetter("O")
    nested.internalInlineSetter("K")
    return inner.internalInlineGetter() + nested.internalInlineGetter()
}
