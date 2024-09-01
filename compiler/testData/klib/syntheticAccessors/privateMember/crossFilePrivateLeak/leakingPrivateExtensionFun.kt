// FILE: A.kt
class A {
    internal inline fun internalInlineMethod() = privateExtension()
}

private fun A.privateExtension() = "OK"

// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod()
}
