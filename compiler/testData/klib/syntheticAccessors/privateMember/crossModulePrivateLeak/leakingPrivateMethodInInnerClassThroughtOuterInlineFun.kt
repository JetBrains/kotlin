// KT-72862: <missing declarations>
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IGNORE_BACKEND_K1: ANY
// MODULE: lib
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

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += A().internalInlineMethodInOuterClass()
    result += internalInlineMethodOutsideOfOuterClass()
    if (result != 2) return result.toString()
    return "OK"
}
