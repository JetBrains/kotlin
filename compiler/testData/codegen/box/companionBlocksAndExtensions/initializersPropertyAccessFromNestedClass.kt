// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""

private fun init(tag: String, value: String): String {
    initLog += tag
    return value
}

class Host {
    companion {
        val value = init("H", "OK")
    }

    class Nested {
        fun read() = value
    }
}

class LateHost {
    companion {
        val trigger = init("L", "lateHost")
        lateinit var late: String
    }

    class Nested {
        fun writeAndRead(value: String): String {
            late = value
            return late
        }
    }
}

fun box(): String {
    val result = Host.Nested().read()
    if (result != "OK") return "FAIL: nested read: $result"
    if (initLog != "H") return "FAIL: order: $initLog"

    val lateResult = LateHost.Nested().writeAndRead("late")
    if (lateResult != "late") return "FAIL: nested lateinit read: $lateResult"
    if (initLog != "HL") return "FAIL: setter order: $initLog"

    return "OK"
}
