// LANGUAGE: +CompanionBlocks +CompanionExtensions

var crossKindLog = ""

class CrossA {
    companion {
        val trigger = run { crossKindLog += "A"; "triggerA" }

        var static_init: String = "userVarA"

        val static_init_called: String = "userPropA"
    }
}

class CrossB {
    companion {
        val trigger = run { crossKindLog += "B"; "triggerB" }

        fun static_init_called(): String = "userFunB"

        fun static_init(): String = "userFunB2"
    }
}

class CrossC {
    companion {
        val trigger = run { crossKindLog += "C"; "triggerC" }

        val static_init: String = "userValC"

        fun static_init_called(): String = "userFunC"
    }
}

fun box(): String {
    if (CrossA.trigger != "triggerA") return "FAIL: CrossA trigger"
    if (CrossA.static_init != "userVarA") return "FAIL: CrossA.static_init read"
    CrossA.static_init = "mutatedA"
    if (CrossA.static_init != "mutatedA") return "FAIL: CrossA.static_init write"
    if (CrossA.static_init_called != "userPropA") return "FAIL: CrossA.static_init_called"

    if (CrossB.trigger != "triggerB") return "FAIL: CrossB trigger"
    if (CrossB.static_init_called() != "userFunB") return "FAIL: CrossB.static_init_called()"
    if (CrossB.static_init() != "userFunB2") return "FAIL: CrossB.static_init()"

    if (CrossC.trigger != "triggerC") return "FAIL: CrossC trigger"
    if (CrossC.static_init != "userValC") return "FAIL: CrossC.static_init"
    if (CrossC.static_init_called() != "userFunC") return "FAIL: CrossC.static_init_called()"

    if (crossKindLog != "ABC") return "FAIL: cross-kind order: $crossKindLog"

    return "OK"
}
