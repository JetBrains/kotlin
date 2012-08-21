class A {
    fun component1() = 1
    fun component2() = 1
}

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in C()) {

    }
}
