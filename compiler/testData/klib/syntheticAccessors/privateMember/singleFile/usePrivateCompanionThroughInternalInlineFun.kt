// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = privateFun()
}

fun box(): String {
    return A().internalInlineFun()
}
