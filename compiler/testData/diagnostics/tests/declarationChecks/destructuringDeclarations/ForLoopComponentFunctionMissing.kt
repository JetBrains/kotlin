class A {
    operator fun component1() = 1
}

class C {
    operator fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((<!UNUSED_VARIABLE!>x<!>, <!UNUSED_VARIABLE!>y<!>) in <!COMPONENT_FUNCTION_MISSING!>C()<!>) {

    }
}
