// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290

class TopLevelInitializerOwner {
    companion {
        private lateinit var late: String
        private val init = initLate()

        fun isInitialized() = ::late.isInitialized

        private fun initLate() {
            late = "late"
        }
    }
}

val topLevelFlag = TopLevelInitializerOwner.isInitialized()

fun box(): String {
    if (!topLevelFlag) return "FAIL"
    return "OK"
}