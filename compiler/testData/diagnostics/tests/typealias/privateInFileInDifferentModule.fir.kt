// ISSUE: KT-62537
// MODULE: m1
// FILE: file1.kt
private class C {
    companion object
}

private typealias TA = C

private val test1: C = C()
private val test1co: C.Companion = C

private val test2: TA = TA()
private val test2co = TA

// MODULE: m2(m1)
// FILE: file2.kt
private val test1: C = C()
private val test1co: C.Companion = <!INITIALIZER_TYPE_MISMATCH, NO_COMPANION_OBJECT!>C<!>

private val test2: TA = <!INVISIBLE_REFERENCE!>TA<!>()
private val test2co = TA

private class C
private typealias TA = Int
