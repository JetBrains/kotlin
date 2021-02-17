// FILE: file1.kt
private class C {
    companion object
}

private typealias TA = C

private val test1: C = <!HIDDEN!>C<!>()
private val test1co: C.Companion = C

private val test2: TA = <!INAPPLICABLE_CANDIDATE!>TA<!>()
private val test2co = TA

// FILE: file2.kt
private val test1: C = C()
private val test1co: C.Companion = C

private val test2: TA = <!INAPPLICABLE_CANDIDATE!>TA<!>()
private val test2co = TA

<!REDECLARATION!>private class C<!>
<!REDECLARATION!>private typealias TA = Int<!>
