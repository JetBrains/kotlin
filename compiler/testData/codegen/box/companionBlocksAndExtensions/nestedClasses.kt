// LANGUAGE: +CompanionBlocksAndExtensions

var initOrder = ""

class Outer {
    companion {
        val outerVal: String = run { initOrder += "O "; "OuterVal" }

        fun outerFun() = "OuterFun"
    }

    class Nested {
        companion {
            val nestedStaticVal: String = run { initOrder += "N "; "NestedVal" }

            fun nestedStaticFun() = "NestedFun"
        }

        fun readNestedVal() = nestedStaticVal
    }
}

fun box(): String {
    // Access nested class companion independently of outer
    if (Outer.Nested.nestedStaticVal != "NestedVal") return "FAIL: nestedStaticVal=${Outer.Nested.nestedStaticVal}"
    if (Outer.Nested.nestedStaticFun() != "NestedFun") return "FAIL: nestedStaticFun=${Outer.Nested.nestedStaticFun()}"

    // Nested init should not trigger outer init
    if (initOrder != "N ") return "FAIL: expected only nested init, got initOrder=$initOrder"

    // Now access outer companion
    if (Outer.outerVal != "OuterVal") return "FAIL: outerVal=${Outer.outerVal}"
    if (Outer.outerFun() != "OuterFun") return "FAIL: outerFun=${Outer.outerFun()}"

    // Now both should be initialized
    if (initOrder != "N O ") return "FAIL: initOrder=$initOrder"

    // Instance access
    val nested = Outer.Nested()
    if (nested.readNestedVal() != "NestedVal") return "FAIL: nested.readNestedVal=${nested.readNestedVal()}"

    return "OK"
}
