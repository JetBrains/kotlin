// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290

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
