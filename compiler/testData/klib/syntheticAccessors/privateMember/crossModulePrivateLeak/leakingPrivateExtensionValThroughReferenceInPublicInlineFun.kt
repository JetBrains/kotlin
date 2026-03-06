// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// MODULE: lib
// FILE: A.kt
private val String.privateVal: String
    get() = this

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = String::privateVal

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke("OK")
}
