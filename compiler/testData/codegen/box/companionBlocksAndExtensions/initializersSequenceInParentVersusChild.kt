// LANGUAGE: +CompanionBlocks

// MODULE: lib
// FILE: lib.kt
var initLog = ""

open class Parent {
    companion {
        val parentValue = run {
            initLog += "P"
            "parent"
        }
    }
}

// MODULE: main(lib)
// FILE: main.kt
class Child : Parent() {
    companion {
        val childValue = run {
            initLog += "C"
            "child"
        }
    }
}

fun box(): String {
    if (Child.childValue != "child") return "FAIL child"
    if (initLog != "PC") return "FAIL order: $initLog"
    if (Parent.parentValue != "parent") return "FAIL parent"

    if (initLog != "PC") return "FAIL order: $initLog"

    return "OK"
}
