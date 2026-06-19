// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val list = mutableListOf<Any>()

// === Regular (non-value) final class inheriting from abstract value class ===

abstract value class AbsVal(a: Int) {
    init {
        list.add("AbsVal($a)")
    }
}

class RegularFinal(val v: Int): AbsVal(v * 2) {
    init {
        list.add("RegularFinal($v)")
    }
}

// === Regular (non-value) final class inheriting from sealed value class with multiple fields ===

sealed value class MSealedForRegular(a: Int, b: Int) {
    init {
        list.add("MSealedForRegular($a,$b)")
    }
}

class RegularFromSealed(val p: Int, val q: Int): MSealedForRegular(p, q) {
    init {
        list.add("RegularFromSealed($p,$q)")
    }
}

// === Regular class through abstract value class chain with multiple fields ===

sealed value class MSealed(a: Int, b: Int) {
    init {
        list.add("MSealed($a,$b)")
    }
}

abstract value class MAbsChain(a: Int, b: Int): MSealed(a, b) {
    init {
        list.add("MAbsChain($a,$b)")
    }
}

class RegularFromChain(val r: Int, val s: Int): MAbsChain(r + 1, s + 1) {
    init {
        list.add("RegularFromChain($r,$s)")
    }
}

fun box(): String {
    // --- Regular final class from abstract value class ---
    list.clear()
    val rf = RegularFinal(5)
    // AbsVal init: a=5*2=10 -> "AbsVal(10)"
    // RegularFinal init: v=5 -> "RegularFinal(5)"
    require(list == listOf("AbsVal(10)", "RegularFinal(5)")) { "RegularFinal: $list" }
    require(rf.v == 5) { "RegularFinal.v = ${rf.v}" }

    // --- Regular final class from sealed value class with multiple fields ---
    list.clear()
    val rfs = RegularFromSealed(7, 8)
    // MSealedForRegular init: a=7, b=8 -> "MSealedForRegular(7,8)"
    // RegularFromSealed init: p=7, q=8 -> "RegularFromSealed(7,8)"
    require(list == listOf("MSealedForRegular(7,8)", "RegularFromSealed(7,8)")) { "RegularFromSealed: $list" }
    require(rfs.p == 7 && rfs.q == 8) { "RegularFromSealed values: ${rfs.p}, ${rfs.q}" }

    // --- Regular class through abstract value class chain with multiple fields ---
    list.clear()
    val rfc = RegularFromChain(10, 20)
    // MSealed init: a=10+1=11, b=20+1=21 -> "MSealed(11,21)"
    // MAbsChain init: a=10+1=11, b=20+1=21 -> "MAbsChain(11,21)"
    // RegularFromChain init: r=10, s=20 -> "RegularFromChain(10,20)"
    require(list == listOf("MSealed(11,21)", "MAbsChain(11,21)", "RegularFromChain(10,20)")) { "RegularFromChain: $list" }
    require(rfc.r == 10 && rfc.s == 20) { "RegularFromChain values: ${rfc.r}, ${rfc.s}" }

    return "OK"
}
