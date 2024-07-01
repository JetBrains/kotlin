// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    private inner class Inner {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Inner().foo()
    internal inline fun internalInlineFun() = privateFun()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}