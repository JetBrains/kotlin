// ISSUE: KT-91246
// IGNORE_BACKEND: NATIVE
// ^ KT-89375 Native: companion block members are not initialized when observed from a nested class

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
        get() = isLateInitialized()
}

// FILE: main.kt
fun box(): String {
    val observer = TopLevelLateinitIndirectObserver()
    if (libFileInitTriggered) return "FAIL 1: file initializers must not run upon instance creation"
    if (!observer.check) return "FAIL 2"
    if (!libFileInitTriggered) return "FAIL 3: file initializers must run upon first ::late.isInitialized evaluation"
    return "OK"
}
