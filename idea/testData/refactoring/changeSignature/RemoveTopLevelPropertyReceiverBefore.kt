package test

public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

class A

open var A.<caret>p: Int
    get() = 1
    set(value: Int) {}

fun test() {
    with(A()) {
        val t = p
        p = 1
    }

    val t1 = A().p
    A().p = 1
}