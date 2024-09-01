// MODULE: lib
// FILE: A.kt
class A {
    class Nested{
        private fun privateMethod() = 1

        internal inline fun internalInlineMethod() = privateMethod()
    }
    @Suppress("INVISIBLE_REFERENCE")
    internal inline fun internalInlineMethodInOuterClass() = Nested().privateMethod()
}

@Suppress("INVISIBLE_REFERENCE")
internal inline fun internalInlineMethodOutsideOfOuterClass() = A.Nested().privateMethod()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    var result = 0
    result += A().internalInlineMethodInOuterClass()
    result += internalInlineMethodOutsideOfOuterClass()
    if (result != 2) return result.toString()
    return "OK"
}
