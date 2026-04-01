// IGNORE_BACKEND: ANY
// IGNORE_KLIB_SYNTHETIC_ACCESSORS_CHECKS: JS_IR, WASM, NATIVE
// The test should be unmuted for JVM when KT-77870 issue is fixed.

// MODULE: lib
// FILE: A.kt
private val String.privateVal: String
    get() = this

private inline fun privateInlineFunction() = String::privateVal
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return transitiveInlineFunction().invoke("OK")
}
