// "Create extension function 'foo'" "false"
// ACTION: Create member function 'foo'
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo("1", 2)
}
