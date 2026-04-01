// IGNORE_BACKEND: ANY
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM, NATIVE
// The test should be unmuted for JVM when KT-77870 issue is fixed.
import kotlin.reflect.KFunction1

// MODULE: lib
// FILE: A.kt
class A private constructor(val s: String) {
    constructor(): this("")

    private inline fun privateInlineFunction(): KFunction1<String, A> = ::A
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().transitiveInlineFunction().invoke("OK").s
}
