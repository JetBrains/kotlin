// "Create member function 'foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): Int? {
    return A().foo(1, "2")
}