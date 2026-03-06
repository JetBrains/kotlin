// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// FILE: A.kt
private fun privateFun() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = ::privateFun

// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke()
}
