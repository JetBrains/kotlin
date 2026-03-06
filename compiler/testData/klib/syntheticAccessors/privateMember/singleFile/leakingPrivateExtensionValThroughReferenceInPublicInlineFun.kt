// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
private val String.privateVal: String
    get() = this

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = String::privateVal

fun box(): String {
    return publicInlineFunction().invoke("OK")
}
