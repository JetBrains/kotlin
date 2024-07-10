// IGNORE_BACKEND: ANY
// ^^^ Muted because accessor for function/constructor/property references are not generated. To be fixed in KT-69797.

// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    public inline fun publicInlineFunction() = ::privateMethod
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke()
}
