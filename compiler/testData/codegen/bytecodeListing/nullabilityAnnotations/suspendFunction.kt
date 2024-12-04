// MODULE: lib
// FILE: 1.kt
interface I {
    fun foo(x: (suspend () -> Unit)?): (suspend () -> Unit)?
}

// MODULE: main(lib)
// FILE: 2.kt
class C : I {
    override fun foo(x: (suspend () -> Unit)?) = x
}
