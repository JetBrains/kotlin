// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// IGNORE_BACKEND: NATIVE
// ^^^ KT-89375 Native: companion block members are not initialized when observed from a top-level initializer

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