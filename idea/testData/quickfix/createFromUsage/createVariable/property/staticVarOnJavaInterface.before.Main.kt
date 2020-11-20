// "Create property 'foo'" "false"
// ACTION: Rename reference
// ACTION: Convert assignment to assignment expression
// ERROR: Unresolved reference: foo

fun test() {
    J.<caret>foo = 1
}
