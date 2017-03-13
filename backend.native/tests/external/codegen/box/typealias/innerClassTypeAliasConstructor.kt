class Outer(val x: String) {
    inner class Inner(val y: String) {
        val z = x + y
    }
}

typealias OI = Outer.Inner

fun box(): String =
        Outer("O").OI("K").z