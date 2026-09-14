// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290, KT-89375
// IGNORE_BACKEND: NATIVE
// ^^^ KT-89375 Native: companion block members are not initialized when observed from a class property initializer

class ClassPropertyInitializerOwner {
    companion {
        private lateinit var late: String
        private val init = initLate()

        private fun initLate() {
            ClassPropertyInitializerOwner.late = "late"
        }
    }

    class UnrelatedClass {
        val flag = ::late.isInitialized
    }
}

fun box(): String {
    if (!ClassPropertyInitializerOwner.UnrelatedClass().flag) return "FAIL"
    return "OK"
}