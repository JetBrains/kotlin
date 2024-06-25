// IGNORE_BACKEND: NATIVE

// FILE: A.kt
class A

// FILE: B.kt
private fun A.privateExtension() = "OK"
internal inline fun A.internalInlineMethod() = privateExtension()

// FILE: main.kt
fun box(): String {
    return A().internalInlineMethod()
}
