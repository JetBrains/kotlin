// "Create function 'foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().foo(1, "2")
}

fun A.foo(i: Int, s: String): Int {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
