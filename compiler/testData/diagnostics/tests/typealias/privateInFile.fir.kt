// FILE: file1.kt
private class C {
    companion object
}

private typealias TA = C

private val test1: <!INVISIBLE_REFERENCE!>C<!> = <!INVISIBLE_REFERENCE!>C<!>()
private val test1co: <!INVISIBLE_REFERENCE!>C<!>.Companion = <!INITIALIZER_TYPE_MISMATCH, INVISIBLE_REFERENCE, NO_COMPANION_OBJECT!>C<!>

private val test2: <!INVISIBLE_REFERENCE!>TA<!> = <!INVISIBLE_REFERENCE!>TA<!>()
private val test2co = <!INVISIBLE_REFERENCE!>TA<!>

// FILE: file2.kt
private val test1: C = C()
private val test1co: C.Companion = <!INITIALIZER_TYPE_MISMATCH, NO_COMPANION_OBJECT!>C<!>

private val test2: TA = <!INVISIBLE_REFERENCE!>TA<!>()
private val test2co = TA

private class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>
private typealias <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TA<!> = Int
