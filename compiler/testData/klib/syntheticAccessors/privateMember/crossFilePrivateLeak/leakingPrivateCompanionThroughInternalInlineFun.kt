// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = privateFun()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}
