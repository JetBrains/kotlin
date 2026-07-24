// LANGUAGE: +CompanionBlocks +CompanionExtensions
class Outer {
    inner class Inner {
        internal inline fun internalInlineMethod() = o
    }

    class Nested {
        internal inline fun internalInlineMethod() = k
    }
}

private companion val Outer.o = "O"
private companion val Outer.k = "K"

fun box(): String {
    return Outer().Inner().internalInlineMethod() + Outer.Nested().internalInlineMethod()
}
