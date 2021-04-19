// FILE: file1.kt
private class C {
    companion object
}

private typealias TA = C

private val test1: C = <!INVISIBLE_REFERENCE!>C<!>()
private val test1co: C.Companion = <!INITIALIZER_TYPE_MISMATCH!>C<!>

private val test2: TA = <!INITIALIZER_TYPE_MISMATCH!>TA(<!NO_VALUE_FOR_PARAMETER!>)<!><!>
private val test2co = TA

// FILE: file2.kt
private val test1: C = C()
private val test1co: C.Companion = <!INITIALIZER_TYPE_MISMATCH!>C<!>

private val test2: TA = <!INITIALIZER_TYPE_MISMATCH!>TA(<!NO_VALUE_FOR_PARAMETER!>)<!><!>
private val test2co = TA

<!REDECLARATION!>private class C<!>
<!REDECLARATION!>private typealias TA = Int<!>
