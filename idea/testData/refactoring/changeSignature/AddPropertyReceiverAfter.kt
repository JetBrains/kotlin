public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

open class A {
    open var <caret>String.p: Int = 1
}

class B: A() {
    override var String.p: Int = 2
}

fun test() {
    with(A()) {
        val t = "".p
        "".p = 1
    }

    with(B()) {
        val t = "".p
        "".p = 2
    }

    with(J()) {
        val t = getP("")
        setP("", 3)
    }
}