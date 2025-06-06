// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: ERROR
// ^^^ When KT-77493 is fixed, this test would become a diagnostic test, and `PARTIAL_LINKAGE_` directives would not be needed

// FILE: A.kt
private fun privateFun() = "OK"

@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
public inline fun publicInlineFunction() = ::privateFun

// FILE: main.kt
fun box(): String {
    return publicInlineFunction().invoke()
}
