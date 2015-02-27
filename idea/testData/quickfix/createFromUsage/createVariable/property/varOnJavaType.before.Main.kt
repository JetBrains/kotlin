// "Create property 'foo'" "true"
// ERROR: Unresolved reference: foo

fun test() {
    A().<caret>foo = ""
}