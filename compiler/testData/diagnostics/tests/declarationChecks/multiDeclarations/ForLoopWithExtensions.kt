class A {
}
fun A.component1() = 1
fun A.component2() = 1

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in C()) {

    }
}
