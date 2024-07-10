// IGNORE_BACKEND: ANY
// ^^^ Muted because accessor for function/constructor/property references are not generated. To be fixed in KT-69797.

// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateMethod
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke()
}
