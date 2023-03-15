// FIR_IDENTICAL
class Outer {
    open inner class Inner(val x: Int)
}

class Host(val y: Int) {
    fun Outer.test() = object : Outer.Inner(42) {
        val xx = x + y
    }
}