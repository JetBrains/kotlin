// "Create extension function 'foo'" "false"
// ACTION: Create function 'foo'
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo("1", 2)
}
