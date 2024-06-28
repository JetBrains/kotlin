// LL_FIR_DIVERGENCE
// Which file `INVISIBLE_REFERENCE` is reported in is unspecified behavior. LL FIR does worse than the compiler in that it doesn't report
// `PACKAGE_OR_CLASSIFIER_REDECLARATION` on either instance of `C` and `TA`, but this is a separate issue: KTIJ-23371.
// LL_FIR_DIVERGENCE

// FILE: file1.kt
private class C {
    companion object
}

private typealias TA = C

private val test1: C = C()
private val test1co: C.Companion = C

private val test2: TA = TA()
private val test2co = TA

// FILE: file2.kt
private val test1: <!INVISIBLE_REFERENCE!>C<!> = <!INVISIBLE_REFERENCE!>C<!>()
private val test1co: <!INVISIBLE_REFERENCE!>C<!>.Companion = <!INVISIBLE_REFERENCE!>C<!>

private val test2: <!INVISIBLE_REFERENCE!>TA<!> = <!INVISIBLE_REFERENCE!>TA<!>()
private val test2co = <!INVISIBLE_REFERENCE!>TA<!>

private class C
private typealias TA = Int
