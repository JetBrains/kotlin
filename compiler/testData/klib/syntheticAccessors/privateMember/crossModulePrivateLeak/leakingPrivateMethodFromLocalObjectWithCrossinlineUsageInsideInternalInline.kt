// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "O"

    internal inline fun internalInlineMethod(crossinline f: () -> String) = object {
        fun run() = privateMethod() + f()
    }.run()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod { "K" }
}
