// "Create function 'foo'" "false"
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo("1", 2)
}
