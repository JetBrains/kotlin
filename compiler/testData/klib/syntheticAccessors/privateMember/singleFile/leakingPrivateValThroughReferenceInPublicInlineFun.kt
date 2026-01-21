// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
class A constructor(val s: String) {
    private val privateVal: String = s

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateVal
}

fun box(): String {
    return A("OK").publicInlineFunction().invoke()
}
