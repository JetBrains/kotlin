// FIR_IDENTICAL
// IGNORE_REVERSED_RESOLVE
// IGNORE_DIAGNOSTIC_API
// KT-62840
val (a, b, c) = A<!NO_VALUE_FOR_PARAMETER!>()<!>

class A(val a: Int) {
    operator fun component1() {}
    operator fun component2() {}
    operator fun component3() {}
}