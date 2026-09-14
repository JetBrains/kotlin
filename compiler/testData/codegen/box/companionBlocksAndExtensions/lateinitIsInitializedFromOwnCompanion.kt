// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375

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
