// FILE: file1.kt
private class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!> {
    companion object
}

private typealias <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TA<!> = C

private val test1: C = C()
private val test1co: C.Companion = C

private val test2: TA = TA()
private val test2co = TA

// FILE: file2.kt
private val test1: <!INVISIBLE_REFERENCE!>C<!> = <!INVISIBLE_MEMBER!>C<!>()
private val test1co: <!INVISIBLE_REFERENCE!>C<!>.<!INVISIBLE_REFERENCE!>Companion<!> = <!INVISIBLE_MEMBER!>C<!>

private val test2: <!INVISIBLE_REFERENCE!>TA<!> = <!INVISIBLE_MEMBER!>TA<!>()
private val test2co = <!INVISIBLE_MEMBER!>TA<!>

private class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>
private typealias <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>TA<!> = Int
