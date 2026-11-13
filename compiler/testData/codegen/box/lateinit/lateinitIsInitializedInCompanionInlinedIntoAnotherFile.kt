// KT-89375
// Companion variant of lateinitIsInitializedInlinedIntoAnotherFile.kt; the copied read goes through
// the companion, so it passes everywhere.

// FILE: lib.kt
class Holder {
    companion object {
        lateinit var late: String

        private val init = initLate()

        private fun initLate() {
            late = "late"
        }

        inline fun probe(): Boolean {
            val o = object {
                val z = ::late.isInitialized
            }
            return o.z
        }
    }
}

// FILE: main.kt
fun box(): String {
    if (!Holder.probe()) return "FAIL"
    return "OK"
}
