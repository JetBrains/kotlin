// "Create member function 'A.Companion.foo'" "true"
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = A.<caret>foo(2)
}