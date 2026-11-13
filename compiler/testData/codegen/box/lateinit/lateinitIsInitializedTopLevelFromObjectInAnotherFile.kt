// KT-89375
// The check is made from an object initializer, triggered from another file.

// FILE: lib.kt
private lateinit var late: String
private val init = initLate()

private fun initLate() {
    late = "late"
}

object TopLevelLateinitObserver {
    val flag = ::late.isInitialized
}

// FILE: main.kt
fun box(): String {
    if (!TopLevelLateinitObserver.flag) return "FAIL"
    return "OK"
}
