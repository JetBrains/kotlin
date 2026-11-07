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

fun isLateInitialized(): Boolean = ::late.isInitialized

class TopLevelLateinitIndirectObserver {
    val check: Boolean
        get() {
            if (libFileInitTriggered) return false
            val result = isLateInitialized()
            if (!libFileInitTriggered) return false
            return result
        }
}

// FILE: main.kt
fun box(): String {
    val observer = TopLevelLateinitIndirectObserver()
    if (libFileInitTriggered) return "FAIL 1: file initializers must not run upon instance creation"
    if (!observer.check) return "FAIL 2"
    if (!libFileInitTriggered) return "FAIL 3: file initializers must run upon first ::late.isInitialized evaluation"
    return "OK"
}
