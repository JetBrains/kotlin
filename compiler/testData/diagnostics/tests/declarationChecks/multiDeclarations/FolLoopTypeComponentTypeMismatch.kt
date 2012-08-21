class A {
    fun component1() = 1
    fun component2() = 1.0
}

class C {
    fun iterator(): Iterator<A> = null!!
}

fun test() {
    for ((x: Double, y: Int) in <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>C()<!>) {

    }
}