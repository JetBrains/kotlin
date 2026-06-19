// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: NATIVE

var initOrder = ""

open class Grandparent {
    companion {
        val gpProp1: String = run { initOrder += "GP1 "; "gp1" }
        val gpProp2: String = run { initOrder += "GP2 "; "gp2" }
    }
}

open class Parent : Grandparent() {
    companion {
        val parentProp1: String = run { initOrder += "P1 "; "p1" }
        val parentProp2: String = run { initOrder += "P2 "; "p2" }
    }
}

class Child : Parent() {
    companion {
        val childProp1: String = run { initOrder += "C1 "; "c1" }
        val childProp2: String = run { initOrder += "C2 "; "c2" }
    }
}

fun box(): String {
    // Accessing a child companion member triggers companion initialization
    // of the entire hierarchy: Grandparent → Parent → Child
    val c1 = Child.childProp1

    if (Grandparent.gpProp1 != "gp1") return "FAIL: gpProp1=${Grandparent.gpProp1}"
    if (Grandparent.gpProp2 != "gp2") return "FAIL: gpProp2=${Grandparent.gpProp2}"
    if (Parent.parentProp1 != "p1") return "FAIL: parentProp1=${Parent.parentProp1}"
    if (Parent.parentProp2 != "p2") return "FAIL: parentProp2=${Parent.parentProp2}"
    if (c1 != "c1") return "FAIL: childProp1=$c1"
    if (Child.childProp2 != "c2") return "FAIL: childProp2=${Child.childProp2}"

    if (initOrder != "GP1 GP2 P1 P2 C1 C2 ") return "FAIL: initOrder=$initOrder"

    return "OK"
}
