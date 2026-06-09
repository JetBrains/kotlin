// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val list = mutableListOf<Any>()

// === Value class with secondary constructors ===

abstract value class AbsSec(a: Int) {
    init {
        list.add("AbsSec($a)")
    }
}

value class WithSecondary(val x: Int) : AbsSec(x * 10) {
    constructor(s: String) : this(s.length) {
        list.add("WithSecondary.secondary($s)")
    }

    init {
        list.add("WithSecondary($x)")
    }
}

// === Constructor delegation chain: secondary → primary → abstract parent ===

abstract value class AbsChainBase(a: Int) {
    init {
        list.add("AbsChainBase($a)")
    }
}

abstract value class AbsChainMid(a: Int, b: Int) : AbsChainBase(a + b) {
    init {
        list.add("AbsChainMid($a,$b)")
    }
}

value class ChainLeaf(val x: Int, val y: Int) : AbsChainMid(x, y) {
    constructor(sum: Int) : this(sum / 2, sum - sum / 2) {
        list.add("ChainLeaf.secondary($sum)")
    }

    init {
        list.add("ChainLeaf($x,$y)")
    }
}

// === Regular class with secondary constructor inheriting from abstract value class ===

abstract value class AbsForRegSec(a: Int) {
    init {
        list.add("AbsForRegSec($a)")
    }
}

class RegularWithSecondary(val v: Int) : AbsForRegSec(v + 1) {
    constructor(s: String) : this(s.length) {
        list.add("RegularWithSecondary.secondary($s)")
    }

    init {
        list.add("RegularWithSecondary($v)")
    }
}

// === Base abstract value class with secondary constructor ===

abstract value class BaseWithSecondary(a: Int) {
    init {
        list.add("BaseWithSecondary.init($a)")
    }

    constructor(a: Int, b: Int) : this(a + b) {
        list.add("BaseWithSecondary.secondary($a,$b)")
    }
}

value class ChildCallsBasePrimary(val x: Int) : BaseWithSecondary(x * 10) {
    init {
        list.add("ChildCallsBasePrimary($x)")
    }
}

value class ChildCallsBaseSecondary(val x: Int) : BaseWithSecondary(x, x * 2) {
    init {
        list.add("ChildCallsBaseSecondary($x)")
    }
}

// Child with its own secondary that ultimately calls base's secondary
value class ChildSecCallsBaseSec(val x: Int) : BaseWithSecondary(x, x + 1) {
    constructor(s: String) : this(s.length) {
        list.add("ChildSecCallsBaseSec.secondary($s)")
    }

    init {
        list.add("ChildSecCallsBaseSec($x)")
    }
}

// Regular class calling base value class secondary constructor
class RegularCallsBaseSec(val v: Int) : BaseWithSecondary(v, v + 5) {
    init {
        list.add("RegularCallsBaseSec($v)")
    }
}

// === Sealed value class with secondary constructor ===

sealed value class SealedWithSecondary(a: Int) {
    init {
        list.add("SealedWithSecondary.init($a)")
    }

    constructor(a: Int, b: Int) : this(a * b) {
        list.add("SealedWithSecondary.secondary($a,$b)")
    }
}

value class SealedChildCallsSec(val x: Int) : SealedWithSecondary(x, x + 1) {
    init {
        list.add("SealedChildCallsSec($x)")
    }
}

fun box(): String {
    // --- Value class with secondary constructor (primary) ---
    list.clear()
    val ws1 = WithSecondary(5)
    // AbsSec init: a=5*10=50 -> "AbsSec(50)"
    // WithSecondary init: x=5 -> "WithSecondary(5)"
    require(list == listOf("AbsSec(50)", "WithSecondary(5)")) { "WithSecondary(primary): $list" }
    require(ws1.x == 5) { "WithSecondary.x = ${ws1.x}" }

    // --- Value class with secondary constructor (secondary) ---
    list.clear()
    val ws2 = WithSecondary("hello")
    // Primary ctor runs first: x=5 (length of "hello")
    // AbsSec init: a=5*10=50 -> "AbsSec(50)"
    // WithSecondary init: x=5 -> "WithSecondary(5)"
    // Secondary body: "WithSecondary.secondary(hello)"
    require(list == listOf("AbsSec(50)", "WithSecondary(5)", "WithSecondary.secondary(hello)")) { "WithSecondary(secondary): $list" }
    require(ws2.x == 5) { "WithSecondary(secondary).x = ${ws2.x}" }

    // --- Constructor delegation chain (primary) ---
    list.clear()
    val cl1 = ChainLeaf(3, 7)
    // AbsChainBase init: a=3+7=10 -> "AbsChainBase(10)"
    // AbsChainMid init: a=3, b=7 -> "AbsChainMid(3,7)"
    // ChainLeaf init: x=3, y=7 -> "ChainLeaf(3,7)"
    require(list == listOf("AbsChainBase(10)", "AbsChainMid(3,7)", "ChainLeaf(3,7)")) { "ChainLeaf(primary): $list" }
    require(cl1.x == 3 && cl1.y == 7) { "ChainLeaf values: ${cl1.x}, ${cl1.y}" }

    // --- Constructor delegation chain (secondary) ---
    list.clear()
    val cl2 = ChainLeaf(10)
    // Primary ctor: x=10/2=5, y=10-5=5
    // AbsChainBase init: a=5+5=10 -> "AbsChainBase(10)"
    // AbsChainMid init: a=5, b=5 -> "AbsChainMid(5,5)"
    // ChainLeaf init: x=5, y=5 -> "ChainLeaf(5,5)"
    // Secondary body: "ChainLeaf.secondary(10)"
    require(list == listOf("AbsChainBase(10)", "AbsChainMid(5,5)", "ChainLeaf(5,5)", "ChainLeaf.secondary(10)")) { "ChainLeaf(secondary): $list" }
    require(cl2.x == 5 && cl2.y == 5) { "ChainLeaf(secondary) values: ${cl2.x}, ${cl2.y}" }

    // --- Regular class with secondary constructor (primary) ---
    list.clear()
    val rws1 = RegularWithSecondary(3)
    // AbsForRegSec init: a=3+1=4 -> "AbsForRegSec(4)"
    // RegularWithSecondary init: v=3 -> "RegularWithSecondary(3)"
    require(list == listOf("AbsForRegSec(4)", "RegularWithSecondary(3)")) { "RegularWithSecondary(primary): $list" }
    require(rws1.v == 3) { "RegularWithSecondary.v = ${rws1.v}" }

    // --- Regular class with secondary constructor (secondary) ---
    list.clear()
    val rws2 = RegularWithSecondary("ab")
    // Primary ctor: v=2 (length of "ab")
    // AbsForRegSec init: a=2+1=3 -> "AbsForRegSec(3)"
    // RegularWithSecondary init: v=2 -> "RegularWithSecondary(2)"
    // Secondary body: "RegularWithSecondary.secondary(ab)"
    require(list == listOf("AbsForRegSec(3)", "RegularWithSecondary(2)", "RegularWithSecondary.secondary(ab)")) { "RegularWithSecondary(secondary): $list" }
    require(rws2.v == 2) { "RegularWithSecondary(secondary).v = ${rws2.v}" }

    // --- Base abstract value class with secondary constructor: child calls primary ---
    list.clear()
    val cbp = ChildCallsBasePrimary(3)
    // BaseWithSecondary.init: a=3*10=30 -> "BaseWithSecondary.init(30)"
    // ChildCallsBasePrimary.init: x=3 -> "ChildCallsBasePrimary(3)"
    require(list == listOf("BaseWithSecondary.init(30)", "ChildCallsBasePrimary(3)")) { "ChildCallsBasePrimary: $list" }
    require(cbp.x == 3) { "ChildCallsBasePrimary.x = ${cbp.x}" }

    // --- Base abstract value class with secondary constructor: child calls secondary ---
    list.clear()
    val cbs = ChildCallsBaseSecondary(4)
    // BaseWithSecondary secondary called: a=4, b=8 -> delegates to primary with a+b=12
    // BaseWithSecondary.init: a=12 -> "BaseWithSecondary.init(12)"
    // BaseWithSecondary.secondary body: "BaseWithSecondary.secondary(4,8)"
    // ChildCallsBaseSecondary.init: x=4 -> "ChildCallsBaseSecondary(4)"
    require(list == listOf("BaseWithSecondary.init(12)", "BaseWithSecondary.secondary(4,8)", "ChildCallsBaseSecondary(4)")) { "ChildCallsBaseSecondary: $list" }
    require(cbs.x == 4) { "ChildCallsBaseSecondary.x = ${cbs.x}" }

    // --- Child with secondary that calls base's secondary (via primary) ---
    list.clear()
    val csbs1 = ChildSecCallsBaseSec(5)
    // BaseWithSecondary secondary: a=5, b=6 -> primary a+b=11
    // BaseWithSecondary.init(11), BaseWithSecondary.secondary(5,6), ChildSecCallsBaseSec(5)
    require(list == listOf("BaseWithSecondary.init(11)", "BaseWithSecondary.secondary(5,6)", "ChildSecCallsBaseSec(5)")) { "ChildSecCallsBaseSec(primary): $list" }
    require(csbs1.x == 5) { "ChildSecCallsBaseSec.x = ${csbs1.x}" }

    // --- Child with secondary that calls base's secondary (via child secondary) ---
    list.clear()
    val csbs2 = ChildSecCallsBaseSec("hi")
    // Child secondary: x = "hi".length = 2
    // Child primary: BaseWithSecondary(2, 3) -> primary(2+3=5)
    // BaseWithSecondary.init(5), BaseWithSecondary.secondary(2,3), ChildSecCallsBaseSec(2), ChildSecCallsBaseSec.secondary(hi)
    require(list == listOf("BaseWithSecondary.init(5)", "BaseWithSecondary.secondary(2,3)", "ChildSecCallsBaseSec(2)", "ChildSecCallsBaseSec.secondary(hi)")) { "ChildSecCallsBaseSec(secondary): $list" }
    require(csbs2.x == 2) { "ChildSecCallsBaseSec(secondary).x = ${csbs2.x}" }

    // --- Regular class calling base value class secondary constructor ---
    list.clear()
    val rcbs = RegularCallsBaseSec(3)
    // BaseWithSecondary secondary: a=3, b=8 -> primary(3+8=11)
    // BaseWithSecondary.init(11), BaseWithSecondary.secondary(3,8), RegularCallsBaseSec(3)
    require(list == listOf("BaseWithSecondary.init(11)", "BaseWithSecondary.secondary(3,8)", "RegularCallsBaseSec(3)")) { "RegularCallsBaseSec: $list" }
    require(rcbs.v == 3) { "RegularCallsBaseSec.v = ${rcbs.v}" }

    // --- Sealed value class with secondary constructor ---
    list.clear()
    val scs = SealedChildCallsSec(3)
    // SealedWithSecondary secondary: a=3, b=4 -> primary(3*4=12)
    // SealedWithSecondary.init(12), SealedWithSecondary.secondary(3,4), SealedChildCallsSec(3)
    require(list == listOf("SealedWithSecondary.init(12)", "SealedWithSecondary.secondary(3,4)", "SealedChildCallsSec(3)")) { "SealedChildCallsSec: $list" }
    require(scs.x == 3) { "SealedChildCallsSec.x = ${scs.x}" }

    return "OK"
}
