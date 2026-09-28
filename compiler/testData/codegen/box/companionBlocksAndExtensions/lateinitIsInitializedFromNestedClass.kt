// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// IGNORE_BACKEND: NATIVE
// ^^^ KT-89375 Native: companion block members are not initialized when observed from a nested class

class I {
    companion {
        private lateinit var late: String
        private val init = initLate()

        private fun initLate() {
            late = "late"
        }
    }

    class NestedClass {
        fun check() = ::late.isInitialized
    }
}

fun box(): String {
    if (!I.NestedClass().check()) return "fail"
    return "OK"
}
