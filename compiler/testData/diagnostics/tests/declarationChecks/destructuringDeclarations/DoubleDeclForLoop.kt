// FIR_IDENTICAL
class A {
    operator fun component1() = 1
    operator fun component2() = 1
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in C()) {

    }
}
