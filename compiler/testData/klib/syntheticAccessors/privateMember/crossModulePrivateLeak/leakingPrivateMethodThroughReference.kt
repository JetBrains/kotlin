// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: ANY
// ^^^ Muted because accessor for function/constructor/property references are not generated. To be fixed in KT-69797.

// KT-72862: Undefined symbols
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

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
