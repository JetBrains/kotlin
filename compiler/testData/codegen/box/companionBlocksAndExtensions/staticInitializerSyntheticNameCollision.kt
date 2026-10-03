// LANGUAGE: +CompanionBlocks +CompanionExtensions

var initLog = ""
var crossKindLog = ""
var inheritanceLog = ""

class Collision {
    companion {
        val trigger = run {
            initLog += "T"
            "trigger"
        }

        fun static_init(): String = "userFun"
    }
}

class CrossA {
    companion {
        val trigger = run { crossKindLog += "A"; "triggerA" }

        var static_init: String = "userVarA"
    }
}

class CrossB {
    companion {
        val trigger = run { crossKindLog += "B"; "triggerB" }

        fun static_init(): String = "userFunB"
    }
}

class CrossC {
    companion {
        val trigger = run { crossKindLog += "C"; "triggerC" }

        val static_init: String = "userValC"
    }
}

class CrossD {
    companion {
        val trigger = run { crossKindLog += "D"; "triggerD" }

        val static_init_state: String = "userValD"
    }
}

class CrossE {
    companion {
        val trigger = run { crossKindLog += "E"; "triggerE" }

        var static_init_state: String = "userVarE"
    }
}

class CrossF {
    companion {
        val trigger = run { crossKindLog += "F"; "triggerF" }

        fun static_init_state(): String = "userFunF"
    }
}

open class Parent {
    companion {
        val parentTrigger = run { inheritanceLog += "P"; "parent" }

        fun static_init(): String = "parentFun"
    }
}

class Child : Parent() {
    companion {
        val childTrigger = run { inheritanceLog += "C"; "child" }

        fun static_init(): String = "childFun"
    }
}

fun box(): String {
    if (Collision.trigger != "trigger") return "FAIL: trigger"
    if (Collision.static_init() != "userFun") return "FAIL: static_init()"
    if (initLog != "T") return "FAIL: order: $initLog"

    if (CrossA.trigger != "triggerA") return "FAIL: CrossA trigger"
    if (CrossA.static_init != "userVarA") return "FAIL: CrossA.static_init read"
    CrossA.static_init = "mutatedA"
    if (CrossA.static_init != "mutatedA") return "FAIL: CrossA.static_init write"

    if (CrossB.trigger != "triggerB") return "FAIL: CrossB trigger"
    if (CrossB.static_init() != "userFunB") return "FAIL: CrossB.static_init()"

    if (CrossC.trigger != "triggerC") return "FAIL: CrossC trigger"
    if (CrossC.static_init != "userValC") return "FAIL: CrossC.static_init"

    if (CrossD.trigger != "triggerD") return "FAIL: CrossD trigger"
    if (CrossD.static_init_state != "userValD") return "FAIL: CrossD.static_init_state"

    if (CrossE.trigger != "triggerE") return "FAIL: CrossE trigger"
    if (CrossE.static_init_state != "userVarE") return "FAIL: CrossE.static_init_state read"
    CrossE.static_init_state = "mutatedE"
    if (CrossE.static_init_state != "mutatedE") return "FAIL: CrossE.static_init_state write"

    if (CrossF.trigger != "triggerF") return "FAIL: CrossF trigger"
    if (CrossF.static_init_state() != "userFunF") return "FAIL: CrossF.static_init_state()"

    if (crossKindLog != "ABCDEF") return "FAIL: cross-kind order: $crossKindLog"

    if (Child.childTrigger != "child") return "FAIL: child trigger"
    if (Parent.parentTrigger != "parent") return "FAIL: parent trigger"
    if (inheritanceLog != "PC") return "FAIL: inheritance order: $inheritanceLog"

    if (Child.static_init() != "childFun") return "FAIL: child fun"
    if (Parent.static_init() != "parentFun") return "FAIL: parent fun"

    return "OK"
}
