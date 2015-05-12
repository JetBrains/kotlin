// "Create extension property 'foo'" "false"
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo
}
