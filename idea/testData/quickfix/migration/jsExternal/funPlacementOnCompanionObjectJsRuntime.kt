// "Create member function 'A.Companion.foo'" "true"
// JS

external class A {
    companion object
}

fun test() {
    A.<caret>foo()
}
