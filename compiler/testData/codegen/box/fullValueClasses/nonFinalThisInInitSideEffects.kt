// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val list = mutableListOf<Any>()

fun captureAny(obj: Any): String = obj.toString()

// === Abstract value class with `this` reference in init block ===

abstract value class AbsWithThisInInit(a: Int) {
    init {
        list.add("AbsWithThisInInit($a)")
        list.add("this=${captureAny(this)}")
    }
}

value class ChildOfAbsWithThis(val x: Int) : AbsWithThisInInit(x * 3) {
    init {
        list.add("ChildOfAbsWithThis($x)")
    }

    override fun toString(): String = "ChildOfAbsWithThis($x)"
}

// === Sealed value class with `this` reference in init block ===

sealed value class SealedWithThisInInit(a: Int) {
    init {
        list.add("SealedWithThisInInit($a)")
        list.add("sealed-this=${captureAny(this)}")
    }
}

value class ChildOfSealedWithThis(val x: Int) : SealedWithThisInInit(x + 10) {
    init {
        list.add("ChildOfSealedWithThis($x)")
    }

    override fun toString(): String = "ChildOfSealedWithThis($x)"
}

fun box(): String {
    // --- Abstract value class with `this` in init block ---
    list.clear()
    val cwt = ChildOfAbsWithThis(5)
    // AbsWithThisInInit.init: a=5*3=15 -> "AbsWithThisInInit(15)"
    // captureAny(this) -> "this=ChildOfAbsWithThis(5)"
    // ChildOfAbsWithThis.init: x=5 -> "ChildOfAbsWithThis(5)"
    require(list == listOf("AbsWithThisInInit(15)", "this=ChildOfAbsWithThis(5)", "ChildOfAbsWithThis(5)")) { "AbsWithThisInInit: $list" }
    require(cwt.x == 5) { "ChildOfAbsWithThis.x = ${cwt.x}" }

    // --- Sealed value class with `this` in init block ---
    list.clear()
    val cswt = ChildOfSealedWithThis(7)
    // SealedWithThisInInit.init: a=7+10=17 -> "SealedWithThisInInit(17)"
    // captureAny(this) -> "sealed-this=ChildOfSealedWithThis(7)"
    // ChildOfSealedWithThis.init: x=7 -> "ChildOfSealedWithThis(7)"
    require(list == listOf("SealedWithThisInInit(17)", "sealed-this=ChildOfSealedWithThis(7)", "ChildOfSealedWithThis(7)")) { "SealedWithThisInInit: $list" }
    require(cswt.x == 7) { "ChildOfSealedWithThis.x = ${cswt.x}" }

    return "OK"
}
