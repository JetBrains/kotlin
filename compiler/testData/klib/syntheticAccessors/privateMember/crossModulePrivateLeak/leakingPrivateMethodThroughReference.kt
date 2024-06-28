// IGNORE_BACKEND: ANY

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
