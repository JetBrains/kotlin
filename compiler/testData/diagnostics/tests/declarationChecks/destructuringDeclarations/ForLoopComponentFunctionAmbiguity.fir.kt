class A {
    operator <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    operator <!CONFLICTING_OVERLOADS!>fun component1()<!> = 1
    operator fun component2() = 1
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in <!COMPONENT_FUNCTION_AMBIGUITY!>C()<!>) {

    }
}
