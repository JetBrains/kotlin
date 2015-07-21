open class A {
    open var <caret>s: String = 1
}

class B: A() {
    override var s: String = 2
}

fun test() {
    val t1 = A().s
    A().s = 1

    val t2 = B().s
    B().s = 2

    val t3 = J().getS()
    J().setS(3)
}