// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// Like lateinitIsInitializedFromNestedClass.kt, but inlined into another file; the copied read goes
// through an accessor that initializes the container.

// FILE: lib.kt
class I {
    companion {
        lateinit var late: String
        private val init = initLate()

        private fun initLate() {
            late = "late"
        }
    }

    class NestedClass {
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
    if (!I.NestedClass().probe()) return "FAIL"
    return "OK"
}
