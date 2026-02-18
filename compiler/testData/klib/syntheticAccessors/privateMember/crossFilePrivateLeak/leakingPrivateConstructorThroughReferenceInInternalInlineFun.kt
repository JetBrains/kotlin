// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
import kotlin.reflect.KFunction1

// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    internal inline fun internalInlineFunction(): KFunction1<String, A> = ::A

    private inline fun privateInlineFunction(): KFunction1<String, A> = ::A
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("O").s + A().transitiveInlineFunction().invoke("K").s
}
