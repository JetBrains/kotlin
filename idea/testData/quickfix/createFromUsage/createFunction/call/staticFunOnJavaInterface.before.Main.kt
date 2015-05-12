// "Create function 'foo'" "false"
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo("1", 2)
}
