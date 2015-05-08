// "Add parameter to function 'f'" "true"
trait OA {
    fun f(a: Int, i: Int)
}

trait OB {
    fun f(a: Int, i: Int)
}

trait O : OA, OB {
    override fun f(a: Int, i: Int)
}

trait OO : O {
    override fun f(a: Int, i: Int) {
    }
}

trait OOO : OO {
    override fun f(a: Int, i: Int) {}
}

trait OOOA : OOO {
    override fun f(a: Int, i: Int) {
    }
}

trait OOOB : OOO {
    override fun f(a: Int, i: Int) {
    }
}

fun usage(o: OA) {
    o.f(1, 12)
}
fun usage(o: OB) {
    o.f(1, 12)
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

fun usage(o: OOOA) {
    o.f(3, 12)
}

fun usage(o: OOOB) {
    o.f(3, 12)
}
