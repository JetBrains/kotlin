// KT-89290
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ KT-89456: Top-level declaration order differs before KLIB serialization vs after deserialization

// The same setup as in lateinitIsInitializedFromNestedClass.kt, but `late` and `init` are top-level.

private lateinit var late: String
private val init = initLate()

private fun initLate() {
    late = "late"
}

class TopLevelLateinitObserver {
    fun check() = ::late.isInitialized
}

fun box(): String {
    if (!TopLevelLateinitObserver().check()) return "FAIL"
    return "OK"
}
