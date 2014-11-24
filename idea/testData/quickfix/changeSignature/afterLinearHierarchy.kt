// "Add parameter to function 'f'" "true"
trait O {
    fun f(a: Int, i: Int)
}

trait OO : O {
    override fun f(a: Int, i: Int) {
    }
}

trait OOO : OO {
    override fun f(a: Int, i: Int) {}
}

fun usage(o: O) {
    o.f(1, 12)
}

fun usage(o: OO) {
    o.f(13, 12)
}

fun usage(o: OOO) {
    o.f(3, 12)
}
