// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_BACKEND: JVM_IR
// JVM_IR KT-85853

var initLog = ""

interface I0 {
    companion {
        val i0Value = run {
            initLog += "I0 "
            "i0"
        }
    }

    fun inheritedDefault(): String = "inherited"
}

interface I1 : I0 {
    companion {
        val i1Value = run {
            initLog += "I1 "
            "i1"
        }
    }

    fun bridgeDefault(): String = inheritedDefault()
}

class C : I1 {
    companion {
        val cValue = run {
            initLog += "C "
            "c"
        }
    }
}

fun box(): String {
    if (C.cValue != "c") return "FAIL: c"
    if (I0.i0Value != "i0") return "FAIL: i0"
    if (I1.i1Value != "i1") return "FAIL: i1"
    if (initLog != "I0 I1 C ") return "FAIL: order: $initLog"

    val c = C()
    if (c.inheritedDefault() != "inherited") return "FAIL: inherited default"
    if (c.bridgeDefault() != "inherited") return "FAIL: bridge default"

    return "OK"
}
