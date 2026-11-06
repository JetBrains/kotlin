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

        val initialized = ::late.isInitialized

        init {
            initLog += "E"
        }
    }
}

fun box(): String {
    if (!Owner.Nested.initialized) return "FAIL1"
    if (initLog != "NOE") return "FAIL2"

    if (!Owner.Nested.initialized) return "FAIL3"
    if (initLog != "NOE") return "FAIL4"

    return "OK"
}
