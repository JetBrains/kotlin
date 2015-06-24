open class A {
    open var <caret>p: Int = 1
}

class B: A() {
    override var p: Int = 2
}

fun test() {
    val t1 = A().p
    A().p = 1

    val t2 = B().p
    B().p = 2

    val t3 = J().getP()
    J().setP(3)
}