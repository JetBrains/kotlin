open class A(open var <caret>p: Int)

class B(override var p: Int): A(p)

class C: A(0) {
    override var p: Int = 1
}

fun test() {
    val t1 = A(0).p
    A(0).p = 1

    val t2 = B(0).p
    B(0).p = 2

    val t3 = C().p
    C().p = 3

    val t4 = J().getP()
    J().setP(4)
}