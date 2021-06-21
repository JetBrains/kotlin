// !JDK_KIND: MODIFIED_MOCK_JDK
// !CHECK_TYPE

interface A : MutableCollection<String> {
    // Override of deprecated function could be marked as deprecated too
    <!NOTHING_TO_OVERRIDE!>override<!> fun nonExistingMethod(x: String) = ""
}

fun foo(x: MutableCollection<Int>, y: Collection<String>, z: A) {
    x.<!UNRESOLVED_REFERENCE!>nonExistingMethod<!>(1).checkType { _<String>() }
    y.<!UNRESOLVED_REFERENCE!>nonExistingMethod<!>("")
    z.nonExistingMethod("")
}
