// !CHECK_TYPE

interface A : MutableCollection<String> {
    // Override of deprecated function could be marked as deprecated too
    override fun nonExistingMethod(x: String) = ""
}

fun foo(x: MutableCollection<Int>, y: Collection<String>, z: A) {
    x.<!DEPRECATION!>nonExistingMethod<!>(1).checkType { _<String>() }
    y.<!DEPRECATION!>nonExistingMethod<!>("")
    z.<!DEPRECATION!>nonExistingMethod<!>("")
}
