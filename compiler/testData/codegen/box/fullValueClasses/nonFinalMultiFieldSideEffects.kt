// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val list = mutableListOf<Any>()

sealed value class MSealed(a: Int, b: Int) {
    init {
        list.add("MSealed($a,$b)")
    }
}

abstract value class MA(a: Int, b: Int, c: Int): MSealed(a + b, c) {
    init {
        list.add("MA($a,$b,$c)")
    }
}

value class MB(val x: Int, val y: Int, val z: Int): MA(x, y, z) {
    init {
        list.add("MB($x,$y,$z)")
    }
}

fun box(): String {
    list.clear()
    val mb = MB(1, 2, 3)
    // MSealed init: a=1+2=3, b=3 -> "MSealed(3,3)"
    // MA init: a=1, b=2, c=3 -> "MA(1,2,3)"
    // MB init: x=1, y=2, z=3 -> "MB(1,2,3)"
    require(list == listOf("MSealed(3,3)", "MA(1,2,3)", "MB(1,2,3)")) { "Multi-field: $list" }
    require(mb.x == 1 && mb.y == 2 && mb.z == 3) { "Multi-field values: ${mb.x}, ${mb.y}, ${mb.z}" }

    return "OK"
}
