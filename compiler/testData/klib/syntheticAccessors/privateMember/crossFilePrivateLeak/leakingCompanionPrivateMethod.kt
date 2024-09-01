// FILE: A.kt
class A {
    companion object{
        private fun privateMethod() = "OK"

        internal inline fun internalInlineCompanionMethod() = privateMethod()
    }

    internal inline fun internalInlineMethod() = privateMethod()
}

// FILE: main.kt
fun box(): String {
    val result = A().internalInlineMethod() + A.internalInlineCompanionMethod()
    if (result != "OKOK") return result
    return "OK"
}
