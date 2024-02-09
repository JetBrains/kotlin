// FIR_IDENTICAL
val (a, b, c) = A<!NO_VALUE_FOR_PARAMETER!>()<!>

class A(val a: Int) {
    operator fun component1() {}
    operator fun component2() {}
    operator fun component3() {}
}