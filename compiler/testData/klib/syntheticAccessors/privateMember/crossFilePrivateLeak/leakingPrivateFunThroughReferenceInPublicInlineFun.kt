// PARTIAL_LINKAGE_MODE: ENABLE
// PARTIAL_LINKAGE_LOG_LEVEL: ERROR
// ^^^ After KT-77493, such constructions will be prohibited, so the test should be moved to diagnostic tests,
//     and `PARTIAL_LINKAGE_` directives removed
// FILE: A.kt
class A {
    private fun privateFun() = "OK"

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    public inline fun publicInlineFunction() = ::privateFun
}

// FILE: main.kt
fun box(): String {
    return A().publicInlineFunction().invoke()
}
