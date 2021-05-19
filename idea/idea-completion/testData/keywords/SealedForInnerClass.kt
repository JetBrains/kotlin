// FIR_IDENTICAL
// FIR_COMPARISON
class A {
    seal<caret>inner class B
}
// ABSENT: "sealed"