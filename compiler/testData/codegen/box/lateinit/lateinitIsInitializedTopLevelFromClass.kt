// KT-89290
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
