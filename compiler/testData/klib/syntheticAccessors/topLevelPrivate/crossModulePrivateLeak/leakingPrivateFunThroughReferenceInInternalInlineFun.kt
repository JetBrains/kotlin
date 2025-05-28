// TARGET_BACKEND: NATIVE, JS_IR, WASM
// The test should be unmuted for JVM when KT-77870 issue is fixed.
// MODULE: lib
// FILE: A.kt
private fun privateFun(s: String) = s

internal inline fun internalInlineFunction() = ::privateFun

private inline fun privateInlineFunction() = ::privateFun
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke("O") + transitiveInlineFunction().invoke("K")
}
