// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// IGNORE_BACKEND: NATIVE
// ^^^ KT-89375 Native: companion block members are not initialized when observed from an anonymous initializer

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