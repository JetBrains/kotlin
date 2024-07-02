// IGNORE_BACKEND: ANY

// FILE: A.kt
class A {
    inner class Inner{
        private fun privateMethod() = 1

        internal inline fun internalInlineMethod() = privateMethod()
    }
    internal inline fun internalInlineMethodInOuterClass() = Inner().privateMethod()
}

internal inline fun internalInlineMethodOutsideOfOuterClass() = A().Inner().privateMethod()

// FILE: main.kt
fun box(): String {
    var result = 0
    result += A().internalInlineMethodInOuterClass()
    result += internalInlineMethodOutsideOfOuterClass()
    if (result != 2) return result.toString()
    return "OK"
}
