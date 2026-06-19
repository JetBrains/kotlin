// LANGUAGE: +CompanionBlocksAndExtensions

var parentInitialized = false

open class Parent {
    companion {
        val parentProp1: String = run { parentInitialized = true; "parent1" }
    }
}

class Child : Parent() {
    companion {
        fun hi(): String { return "hi"}
    }
}

fun box(): String {
    // Accessing a child companion member triggers parent companion initialization, even if child doesn't have static initializers
    val c1 = Child.hi()

    if (Parent.parentProp1 != "parent1") return "FAIL: parentProp1=${Parent.parentProp1}"
    if (!parentInitialized) return "FAIL: parentInitialized=${parentInitialized}"

    return "OK"
}
