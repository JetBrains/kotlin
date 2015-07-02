open class A(open var <caret>s: String)

class B(override var s: String): A(s)

class C: A(0) {
    override var s: String = 1
}

fun test() {
    val t1 = A(0).s
    A(0).s = 1

    val t2 = B(0).s
    B(0).s = 2

    val t3 = C().s
    C().s = 3

    val t4 = J().getS()
    J().setS(4)
}