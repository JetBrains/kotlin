// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    public inline fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateMethod<!>
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke()
}
