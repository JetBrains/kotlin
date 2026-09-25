// ISSUE: KT-89290
// WITH_STDLIB
// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""

class C {
    companion {
        val trigger = run {
            initLog += "T"
            "trigger"
        }

        lateinit var late: String

        fun isLateInitialized() = ::late.isInitialized
    }
}

fun box(): String {
    if (C.isLateInitialized()) return "FAIL: late before assignment"
    if (initLog != "T") return "FAIL: late guard: $initLog"

    C.late = "OK"

    if (!C.isLateInitialized()) return "FAIL: late after assignment"
    if (C.trigger != "trigger") return "FAIL: trigger"
    if (initLog != "T") return "FAIL: double init: $initLog"

    return C.late
}
