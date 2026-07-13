// LANGUAGE: +CompanionBlocksAndExtensions

var initOrder = ""

fun trackInit(tag: String, value: String): String {
    initOrder += "$tag "
    return value
}

class Outer {
    companion {
        val outerVal: String = trackInit("O", "OuterVal")
        fun outerFun() = "OuterFun"
    }

    inner class Inner {
        fun readOuterVal() = outerVal
        fun readOuterFun() = outerFun()

        companion {
            val innerStaticVal: String = trackInit("I", "InnerStaticVal")
            fun innerStaticFun() = "InnerStaticFun"
        }
    }
}

fun box(): String {
    // Access inner companion block first — check if it triggers outer init too
    val innerVal = Outer.Inner.innerStaticVal
    if (innerVal != "InnerStaticVal") return "FAIL: innerStaticVal=$innerVal"
    if (Outer.Inner.innerStaticFun() != "InnerStaticFun") return "FAIL: innerStaticFun=${Outer.Inner.innerStaticFun()}"

    // Record what has been initialized so far (inner companion init only)
    val afterInner = initOrder

    // Now access outer companion
    if (Outer.outerVal != "OuterVal") return "FAIL: outerVal=${Outer.outerVal}"
    if (Outer.outerFun() != "OuterFun") return "FAIL: outerFun=${Outer.outerFun()}"

    // Verify initialization order:
    // Inner class companion is independent of outer — accessing Inner companion
    // should not trigger Outer companion initialization (similar to nested classes).
    if (afterInner != "I ") return "FAIL: expected only inner init first, got afterInner=$afterInner"
    if (initOrder != "I O ") return "FAIL: expected IO, got initOrder=$initOrder"

    // Access outer companion from inner class instance
    val outer = Outer()
    val inner = outer.Inner()
    if (inner.readOuterVal() != "OuterVal") return "FAIL: inner.readOuterVal=${inner.readOuterVal()}"
    if (inner.readOuterFun() != "OuterFun") return "FAIL: inner.readOuterFun=${inner.readOuterFun()}"

    return "OK"
}
