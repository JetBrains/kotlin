// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: A.kt
class A {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun internalInlineFun() = privateFun()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}
