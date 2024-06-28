// FIR_IDENTICAL
// ISSUE: KT-68521
// DIAGNOSTICS: -DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST

open class A {
    var a = 10
        private set
}

interface B {
    var a: Int
}

fun A.foo() {
    if (this is B) {
        a = 20
        a += 5
        a++
        ++a
    }
}
