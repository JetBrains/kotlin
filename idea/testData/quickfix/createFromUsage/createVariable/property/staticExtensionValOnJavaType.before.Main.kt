// "Create extension property 'foo'" "false"
// ACTION: Create property 'foo'
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo
}
