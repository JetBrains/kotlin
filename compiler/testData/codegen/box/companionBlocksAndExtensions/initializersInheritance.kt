// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: NATIVE

var initOrder = ""

open class Parent {
    companion {
        val parentProp1: String = run { initOrder += "P1 "; "parent1" }
        val parentProp2: String = run { initOrder += "P2 "; "parent2" }
    }
}

class Child : Parent() {
    companion {
        val childProp1: String = run { initOrder += "C1 "; "child1" }
        val childProp2: String = run { initOrder += "C2 "; "child2" }
    }
}

fun box(): String {
    // Accessing a child companion member triggers companion initialization.
    // §3.3: companion initialization of a class triggers companion initialization
    // of its parent classes first.
    // §3.2: initialization respects program order within each class.
    val c1 = Child.childProp1

    // Parent properties must be initialized (static initializer was called)
    if (Parent.parentProp1 != "parent1") return "FAIL: parentProp1=${Parent.parentProp1}"
    if (Parent.parentProp2 != "parent2") return "FAIL: parentProp2=${Parent.parentProp2}"

    // Child properties must be initialized
    if (c1 != "child1") return "FAIL: childProp1=$c1"
    if (Child.childProp2 != "child2") return "FAIL: childProp2=${Child.childProp2}"

    // Parent initializers run before child initializers, both in program order
    if (initOrder != "P1 P2 C1 C2 ") return "FAIL: initOrder=$initOrder"

    return "OK"
}
