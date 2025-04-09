// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: lib
// FILE: A.kt
class A {
    private fun privateMethod() = "OK"

    public inline fun publicInlineFunction() = ::<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateMethod<!>
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return "OK" // No need in callsite of `A().publicInlineFunction().invoke()`, since diagnostic pipeline would have deserialization issues
}
