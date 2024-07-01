class A {
    private inner class Inner {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Inner().foo()
    internal inline fun internalInlineFun() = privateFun()
}

fun box(): String {
    return A().internalInlineFun()
}