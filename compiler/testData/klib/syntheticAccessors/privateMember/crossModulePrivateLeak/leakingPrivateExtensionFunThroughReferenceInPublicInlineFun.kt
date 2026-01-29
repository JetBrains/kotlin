// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// MODULE: lib
// FILE: A.kt
private fun Int.privateExtensionFun() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = Int::privateExtensionFun

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke(1)
}
