// ISSUE: KT-91246

// FILE: state.kt
var fileInitTriggered = false

// FILE: lib.kt
private val sideEffect = run {
    fileInitTriggered = true
}

class MemberLateinitObserver {
    lateinit var memberLate: String

    fun check(): Boolean = ::memberLate.isInitialized
}

// FILE: main.kt
fun box(): String {
    val observer = MemberLateinitObserver()
    if (observer.check()) return "FAIL 1"
    if (fileInitTriggered) return "FAIL 2: file initialization triggered prematurely"
    observer.memberLate = "OK"
    if (!observer.check()) return "FAIL 3"
    if (fileInitTriggered) return "FAIL 4: file initialization triggered prematurely"
    return "OK"
}
