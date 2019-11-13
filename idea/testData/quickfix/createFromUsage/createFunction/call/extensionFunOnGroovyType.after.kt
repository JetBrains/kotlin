// "Create extension function 'A.foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int {
    return A().foo()
}

private fun A.foo(): Int {
    TODO("Not yet implemented")
}
