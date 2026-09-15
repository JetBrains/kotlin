// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// IGNORE_BACKEND: NATIVE
// ^^^ KT-89375 Native: companion block members are not initialized when observed from its own companion

class OwnCompanionOwner {
    companion {
        private lateinit var late: String
        private val init = initLate()
        val flag = ::late.isInitialized

        private fun initLate() {
            OwnCompanionOwner.late = "late"
        }
    }
}

fun box(): String {
    if (!OwnCompanionOwner.flag) return "FAIL"
    return "OK"
}