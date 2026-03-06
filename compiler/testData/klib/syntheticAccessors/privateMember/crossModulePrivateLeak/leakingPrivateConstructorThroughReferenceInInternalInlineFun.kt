// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// TARGET_BACKEND: NATIVE, JS_IR, WASM
// The test should be unmuted for JVM when KT-77870 issue is fixed.
import kotlin.reflect.KFunction1

// MODULE: lib
// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    internal inline fun internalInlineFunction(): KFunction1<String, A> = ::A

    private inline fun privateInlineFunction(): KFunction1<String, A> = ::A
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("O").s + A().transitiveInlineFunction().invoke("K").s
}
