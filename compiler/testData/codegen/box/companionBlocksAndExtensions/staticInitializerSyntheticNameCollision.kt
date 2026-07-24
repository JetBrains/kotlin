// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""

class Collision {
    companion {
        val trigger = run {
            initLog += "T"
            "trigger"
        }

        fun static_init(): String = "userFun"
        val static_init_called: String = "userProperty"
    }
}

fun box(): String {
    if (Collision.trigger != "trigger") return "FAIL: trigger"
    if (Collision.static_init() != "userFun") return "FAIL: static_init()"
    if (Collision.static_init_called != "userProperty") return "FAIL: static_init_called"
    if (initLog != "T") return "FAIL: order: $initLog"

    return "OK"
}
