// KT-89375
// Like lateinitIsInitializedTopLevelFromClass.kt, but the check is triggered from another file.

// FILE: lib.kt
private lateinit var late: String
private val init = initLate()

private fun initLate() {
    late = "late"
}

class TopLevelLateinitObserver {
    fun check() = ::late.isInitialized
}

// FILE: main.kt
fun box(): String {
    if (!TopLevelLateinitObserver().check()) return "FAIL"
    return "OK"
}
