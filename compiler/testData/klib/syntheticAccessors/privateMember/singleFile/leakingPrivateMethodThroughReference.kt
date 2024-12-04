// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class A {
    private fun privateMethod() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateMethod
}

fun box(): String {
    return A().publicInlineFunction().invoke()
}
