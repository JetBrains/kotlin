// "Remove parameter 'a'" "true"
trait OA {
    fun f(a: Int)
}

trait OB {
    fun f(a: Int)
}

trait O : OA, OB {
    override fun f(a: Int)
}

trait OO : O {
    override fun f(a: Int) {
    }
}

trait OOO : OO {
    override fun f(a: Int) {}
}

trait OOOA : OOO {
    override fun f(a: Int) {
    }
}

trait OOOB : OOO {
    override fun f(a: Int) {
    }
}

fun usage(o: OA) {
    o.f(<caret>)
}
fun usage(o: OB) {
    o.f(1)
}

fun usage(o: O) {
    o.f(1)
}

fun usage(o: OO) {
    o.f(13)
}

fun usage(o: OOO) {
    o.f(3)
}

fun usage(o: OOOA) {
    o.f(3)
}

fun usage(o: OOOB) {
    o.f(3)
}
