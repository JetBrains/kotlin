// !JDK_KIND: MODIFIED_MOCK_JDK
// !CHECK_TYPE

interface A : MutableCollection<String> {
    // Override of deprecated function could be marked as deprecated too
    override fun nonExistingMethod(x: String) = ""
}

fun foo(x: MutableCollection<Int>, y: Collection<String>, z: A) {
    x.<!UNRESOLVED_REFERENCE!>nonExistingMethod<!>(1).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>checkType<!> { _<String>() }
    y.<!UNRESOLVED_REFERENCE!>nonExistingMethod<!>("")
    z.<!UNRESOLVED_REFERENCE!>nonExistingMethod<!>("")
}
