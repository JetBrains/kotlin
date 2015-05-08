// "Remove parameter 'a'" "true"
trait OA {
    fun f()
}

trait OB {
    fun f(a: Int)
}

trait O : OA, OB {
    override fun f()
}

trait OO : O {
    override fun f() {
    }
}

trait OOO : OO {
    override fun f() {}
}

trait OOOA : OOO {
    override fun f() {
    }
}

trait OOOB : OOO {
    override fun f() {
    }
}

fun usage(o: OA) {
    o.f()
}
fun usage(o: OB) {
    o.f(1)
}

fun usage(o: O) {
    o.f()
}

fun usage(o: OO) {
    o.f()
}

fun usage(o: OOO) {
    o.f()
}

fun usage(o: OOOA) {
    o.f()
}

fun usage(o: OOOB) {
    o.f()
}
