// IGNORE_BACKEND: ANY
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM, NATIVE
// The test should be unmuted for JVM when KT-77870 issue is fixed.
// MODULE: lib
// FILE: A.kt
private fun privateFun(s: String) = s

private inline fun privateInlineFunction() = ::privateFun
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return transitiveInlineFunction().invoke("OK")
}
