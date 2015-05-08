// "Add parameter to function 'f'" "true"
trait OA {
    fun f(a: Int, s: String)
}

trait OB {
    fun f(a: Int, s: String)
}

trait O : OA, OB {
    override fun f(a: Int, s: String)
}

trait OO : O {
    override fun f(a: Int, s: String) {
    }
}

trait OOO : OO {
    override fun f(a: Int, s: String) {}
}

trait OOOA : OOO {
    override fun f(a: Int, s: String) {
    }
}

trait OOOB : OOO {
    override fun f(a: Int, s: String) {
    }
}

fun usage(o: OA) {
    o.f(1, "asdv")
}
fun usage(o: OB) {
    o.f(1, "asdv")
}

fun usage(o: O) {
    o.f(1, "asdv")
}

fun usage(o: OO) {
    o.f(13, "asdv")
}

fun usage(o: OOO) {
    o.f(3, "asdv")
}

fun usage(o: OOOA) {
    o.f(3, "asdv")
}

fun usage(o: OOOB) {
    o.f(3, "asdv")
}
