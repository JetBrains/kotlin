// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
import kotlin.reflect.KFunction1

// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction(): KFunction1<String, A> = ::A
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke("OK").s
}
