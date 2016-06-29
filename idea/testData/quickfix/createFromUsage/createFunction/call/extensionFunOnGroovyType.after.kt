// "Create extension function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().foo()
}

fun A.foo(): Int {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}
