// "Create member property 'A.Companion.foo'" "true"
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = A.<caret>foo
}