class A {
    fun component1() = 1
}

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x) in C()) {

    }
}