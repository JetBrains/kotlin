// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class A {
    private class Nested {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Nested().foo()
    internal inline fun internalInlineFun() = privateFun()
}

fun box(): String {
    return A().internalInlineFun()
}