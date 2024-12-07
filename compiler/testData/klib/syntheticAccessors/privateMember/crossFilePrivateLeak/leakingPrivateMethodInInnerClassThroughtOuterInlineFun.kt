// IGNORE_BACKEND_K1: ANY
// FILE: A.kt
class A {
    inner class Inner{
        private fun privateMethod() = 1

        internal inline fun internalInlineMethod() = privateMethod()
    }
    @Suppress("INVISIBLE_REFERENCE")
    internal inline fun internalInlineMethodInOuterClass() = Inner().privateMethod()
}

@Suppress("INVISIBLE_REFERENCE")
internal inline fun internalInlineMethodOutsideOfOuterClass() = A().Inner().privateMethod()

// FILE: main.kt
fun box(): String {
    var result = 0
    result += A().internalInlineMethodInOuterClass()
    result += internalInlineMethodOutsideOfOuterClass()
    if (result != 2) return result.toString()
    return "OK"
}
