class A {
}
operator fun A.component1() = 1
operator fun A.component2() = 1

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, <!UNUSED_VARIABLE!>y<!>) in C()) {

    }
}
