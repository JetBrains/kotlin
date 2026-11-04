// ISSUE: KT-91246

// FILE: state.kt
var libFileInitTriggered = false

// FILE: lib.kt
private lateinit var late: String
private val init = initLate()

private fun initLate() {
    libFileInitTriggered = true
    late = "late"
}

class TopLevelLateinitInitObserver {
    val check: Boolean

    init {
        check = ::late.isInitialized
    }
}

// FILE: main.kt
fun box(): String {
    val observer = TopLevelLateinitInitObserver()
    if (!libFileInitTriggered) return "FAIL 1: file initializers must run upon instance creation"
    if (!observer.check) return "FAIL 2"
    return "OK"
}
