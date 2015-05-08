// "Add parameter to function 'f'" "true"
trait O {
    fun f(a: Int)
}

trait OO : O {
    override fun f(a: Int) {
    }
}

trait OOO : OO {
    override fun f(a: Int) {}
}

fun usage(o: O) {
    o.f(1)
}

fun usage(o: OO) {
    o.f(13, <caret>12)
}

fun usage(o: OOO) {
    o.f(3)
}
