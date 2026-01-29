// LANGUAGE: -ForbidExposingLessVisibleTypesInInline
// TARGET_BACKEND: NATIVE, JS_IR, WASM
// The test should be unmuted for JVM when KT-77870 issue is fixed.
// MODULE: lib
// FILE: A.kt
private fun Int.privateExtensionFun(s: String) = s

internal inline fun internalInlineFunction() = Int::privateExtensionFun

private inline fun privateInlineFunction() = Int::privateExtensionFun
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().invoke(1, "O") + transitiveInlineFunction().invoke(1, "K")
}
