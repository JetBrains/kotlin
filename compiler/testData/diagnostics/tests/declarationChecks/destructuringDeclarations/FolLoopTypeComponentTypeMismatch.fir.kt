// !WITH_NEW_INFERENCE
class A {
    operator fun component1() = 1
    operator fun component2() = 1.0
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x: Double, y: Int) in C()) {

    }
}
