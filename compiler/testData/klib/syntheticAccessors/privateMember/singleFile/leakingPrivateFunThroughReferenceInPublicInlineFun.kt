// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
class A {
    private fun privateFun() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateFun
}

fun box(): String {
    return A().publicInlineFunction().invoke()
}
