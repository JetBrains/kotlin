// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: JVM, JVM_IR, NATIVE

var initOrder = ""

open class Parent {
    companion {
        val parentBlock1: String = run { initOrder += "PB1 "; "pb1" }
    }

    companion object {
        val parentObj: String = run { initOrder += "PO "; "po" }
    }

    companion {
        val parentBlock2: String = run { initOrder += "PB2 "; "pb2" }
    }
}

class Child : Parent() {
    companion {
        val childBlock1: String = run { initOrder += "CB1 "; "cb1" }
    }

    companion object {
        val childObj: String = run { initOrder += "CO "; "co" }
    }

    companion {
        val childBlock2: String = run { initOrder += "CB2 "; "cb2" }
    }
}

fun box(): String {
    // Accessing child member triggers parent init first, then child init.
    // Within each class, program order is respected.
    val cb1 = Child.childBlock1

    if (Parent.parentBlock1 != "pb1") return "FAIL: parentBlock1=${Parent.parentBlock1}"
    if (Parent.parentObj != "po") return "FAIL: parentObj=${Parent.Companion.parentObj}"
    if (Parent.parentBlock2 != "pb2") return "FAIL: parentBlock2=${Parent.parentBlock2}"
    if (cb1 != "cb1") return "FAIL: childBlock1=$cb1"
    if (Child.childObj != "co") return "FAIL: childObj=${Child.Companion.childObj}"
    if (Child.childBlock2 != "cb2") return "FAIL: childBlock2=${Child.childBlock2}"

    if (initOrder != "PB1 PO PB2 CB1 CO CB2 ") return "FAIL: initOrder=$initOrder"

    return "OK"
}
