// FILE: A.kt
class A {
    private fun privateFun() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateFun
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke()
}
