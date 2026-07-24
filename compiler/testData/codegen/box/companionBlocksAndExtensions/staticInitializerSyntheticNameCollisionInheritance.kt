// LANGUAGE: +CompanionBlocks +CompanionExtensions
var inheritanceLog = ""

open class Parent {
    companion {
        val parentTrigger = run { inheritanceLog += "P"; "parent" }

        fun static_init(): String = "parentFun"
        val static_init_called: String = "parentProp"
    }
}

class Child : Parent() {
    companion {
        val childTrigger = run { inheritanceLog += "C"; "child" }

        fun static_init(): String = "childFun"
        val static_init_called: String = "childProp"
    }
}

fun box(): String {
    if (Child.childTrigger != "child") return "FAIL: child trigger"
    if (Parent.parentTrigger != "parent") return "FAIL: parent trigger"
    if (inheritanceLog != "PC") return "FAIL: inheritance order: $inheritanceLog"

    if (Child.static_init() != "childFun") return "FAIL: child fun"
    if (Parent.static_init() != "parentFun") return "FAIL: parent fun"
    if (Child.static_init_called != "childProp") return "FAIL: child prop"
    if (Parent.static_init_called != "parentProp") return "FAIL: parent prop"

    return "OK"
}
