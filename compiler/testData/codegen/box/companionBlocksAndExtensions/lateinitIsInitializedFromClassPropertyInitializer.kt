// LANGUAGE: +CompanionBlocks +CompanionExtensions
// KT-89290

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