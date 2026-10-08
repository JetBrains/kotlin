// LANGUAGE: +CompanionBlocks
// ISSUE: KT-89290
// IGNORE_BACKEND: NATIVE
// KT-89375: Native misses companion initialization on nested lateinit checks.

private var hits = 0

class Owner {
    companion {
        private lateinit var late: String
        private val marker = initializeLate()

        private fun initializeLate() {
            hits++
            late = "OK"
        }
    }

    class Nested {
        fun observer(): () -> Boolean = { ::late.isInitialized }
    }
}

fun box(): String {
    val check = Owner.Nested().observer()
    if (!check()) return "FAIL1"
    return if (hits == 1) "OK" else "FAIL2"
}
