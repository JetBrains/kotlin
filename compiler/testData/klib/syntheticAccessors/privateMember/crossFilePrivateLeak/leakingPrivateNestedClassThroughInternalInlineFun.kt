// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    private class Nested {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Nested().foo()
    internal inline fun internalInlineFun() = privateFun()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}