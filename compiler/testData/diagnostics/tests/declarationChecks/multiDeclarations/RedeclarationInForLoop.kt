class A {
    operator fun component1() = 1
    operator fun component2() = 1
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((<!REDECLARATION!>x<!>, <!NAME_SHADOWING, REDECLARATION, UNUSED_VARIABLE!>x<!>) in C()) {

    }
}
