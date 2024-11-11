// MODULE: lib
// FILE: A.kt
class A {
    companion object {
        private const val privateConstVal = "OK"
    }
    internal inline fun internalInlineMethod() = privateConstVal
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod()
}