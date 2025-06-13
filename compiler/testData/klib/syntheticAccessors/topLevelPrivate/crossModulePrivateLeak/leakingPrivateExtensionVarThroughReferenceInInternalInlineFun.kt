// TARGET_BACKEND: NATIVE
// The test should be unmuted for JVM when KT-77870 issue is fixed.
// The test should be unmuted for JS when KT-76093 issue is fixed.

// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: ERROR
// ^^^ When KT-77493 is fixed, this test would become a diagnostic test, and `PARTIAL_LINKAGE_` directives would not be needed

// MODULE: lib
// FILE: A.kt
private var privateVar: String = ""

internal inline fun internalInlineFunction() = ::privateVar

private inline fun privateInlineFunction() = ::privateVar
internal inline fun transitiveInlineFunction() = privateInlineFunction()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFunction().apply { set("O") }.get() + transitiveInlineFunction().apply { set("K") }.get()
}
