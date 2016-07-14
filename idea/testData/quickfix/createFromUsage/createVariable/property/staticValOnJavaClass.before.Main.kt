// "Create member property 'J.foo'" "true"
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo
}

