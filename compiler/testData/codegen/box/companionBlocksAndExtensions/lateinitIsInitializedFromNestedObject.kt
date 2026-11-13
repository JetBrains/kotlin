// LANGUAGE: +CompanionBlocks
// ISSUE: KT-89290

private var initLog = ""

class Owner {
    companion {
        private lateinit var late: String
        private val marker = initializeLate()

        private fun initializeLate() {
            initLog += "O"
            late = "OK"
        }
    }

    object Nested {
        init {
            initLog += "N"
        }

        fun touch() = "nested"
        fun check() = ::late.isInitialized
    }
}

fun box(): String {
    if (Owner.Nested.touch() != "nested") return "FAIL1"
    if (initLog != "N") return "FAIL2"

    if (!Owner.Nested.check()) return "FAIL3"
    if (initLog != "NO") return "FAIL4"

    if (!Owner.Nested.check()) return "FAIL5"
    if (initLog != "NO") return "FAIL6"

    return "OK"
}
