// "Create extension property 'foo'" "false"
// ACTION: Create member property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo
}
