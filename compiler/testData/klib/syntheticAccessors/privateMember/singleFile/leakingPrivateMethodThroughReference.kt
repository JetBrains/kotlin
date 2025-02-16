// DONT_TARGET_EXACT_BACKEND: JS_IR
// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// ^^^ Muted because accessor for function/constructor/property references are not generated for JS and first stage. To be fixed in KT-69797.
// Can be replaced with ignore after KT-69941

class A {
    private fun privateMethod() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateMethod
}

fun box(): String {
    return A().publicInlineFunction().invoke()
}
