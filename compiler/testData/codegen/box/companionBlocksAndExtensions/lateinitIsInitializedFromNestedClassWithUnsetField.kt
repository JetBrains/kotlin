// LANGUAGE: +CompanionBlocks
// ISSUE: KT-89290
// IGNORE_BACKEND: NATIVE
// KT-89375: Native misses companion initialization on nested lateinit checks.

private var hits = 0

class Owner {
    companion {
        private lateinit var late: String
        private val marker = countInitialization()

        private fun countInitialization() {
            hits++
        }

        fun assign() {
            late = "OK"
        }
    }

    class Nested {
        fun check() = ::late.isInitialized
    }
}

fun box(): String {
    val n = Owner.Nested()
    if (n.check()) return "FAIL1"
    if (hits != 1) return "FAIL2"
    Owner.assign()
    if (!n.check() || hits != 1) return "FAIL3"
    return "OK"
}
