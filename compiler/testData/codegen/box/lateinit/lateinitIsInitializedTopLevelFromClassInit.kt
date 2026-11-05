// ISSUE: KT-91246
// IGNORE_BACKEND: NATIVE

// FILE: lib.kt
private lateinit var late: String
private val init = initLate()

private fun initLate() {
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
    if (!TopLevelLateinitInitObserver().check) return "FAIL"
    return "OK"
}
