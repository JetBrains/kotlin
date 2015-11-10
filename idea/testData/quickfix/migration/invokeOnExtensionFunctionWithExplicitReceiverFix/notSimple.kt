// "Surround callee with parenthesis" "true"

class A {
    val foo: B.(String, Int) -> Unit get() = null!!
}

class B {
    fun getA() = A()
}


fun test(b: B) {
    with(b) {
        b.getA().<caret>foo("", 1)
    }
}

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()