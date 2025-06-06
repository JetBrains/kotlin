// TARGET_BACKEND: NATIVE, JS_IR, WASM
// The test should be unmuted for JVM when KT-77870 issue is fixed.
// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: ERROR
// ^^^ After KT-77493, such constructions will be prohibited, so the test should be moved to diagnostic tests,
//     and `PARTIAL_LINKAGE_` directives removed
// MODULE: lib
// FILE: A.kt
class A {
    private fun privateFun(s: String) = s

    internal inline fun internalInlineFunction() = ::privateFun

    private inline fun privateInlineFunction() = ::privateFun
    internal inline fun transitiveInlineFunction() = privateInlineFunction()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFunction().invoke("O") + A().transitiveInlineFunction().invoke("K")
}
