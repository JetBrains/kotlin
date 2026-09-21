// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290

class AnonymousInitializerOwner {
    companion {
        private lateinit var late: String
        private val init = initLate()

        private fun initLate() {
            AnonymousInitializerOwner.late = "late"
        }
    }

    class UnrelatedClass {
        val flag: Boolean

        init {
            flag = ::late.isInitialized
        }
    }
}

fun box(): String {
    if (!AnonymousInitializerOwner.UnrelatedClass().flag) return "FAIL"
    return "OK"
}