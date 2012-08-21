class A {
    fun component1() = 1
}

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x, y) in <!COMPONENT_FUNCTION_MISSING!>C()<!>) {

    }
}
