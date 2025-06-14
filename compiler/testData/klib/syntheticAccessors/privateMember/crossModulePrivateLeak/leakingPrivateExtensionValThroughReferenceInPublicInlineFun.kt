// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: ERROR
// ^^^ When KT-77493 is fixed, this test would become a diagnostic test, and `PARTIAL_LINKAGE_` directives would not be needed

// IGNORE_BACKEND: JS_IR
// The test should be unmuted for JS when KT-76093 issue is fixed.
// MODULE: lib
// FILE: A.kt
private val String.privateVal: String
    get() = this

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = String::privateVal

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke("OK")
}
