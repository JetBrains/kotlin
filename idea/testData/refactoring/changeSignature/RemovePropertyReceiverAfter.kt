open class A {
    open var p: Int = 1
}

class AA : A() {
    override var p: Int = 1
}

class B : J() {
    override var p: Int = 1
}

fun test() {
    with(A()) {
        val t = p
        p = 3
    }

    with(AA()) {
        val t = p
        p = 3
    }

    with(J()) {
        val t = p
        p = 3
    }

    with(B()) {
        val t = p
        p = 3
    }
}