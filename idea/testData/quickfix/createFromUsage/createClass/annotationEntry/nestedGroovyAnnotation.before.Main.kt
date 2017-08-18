// "Create annotation 'foo'" "false"
// ERROR: Unresolved reference: foo
// ACTION: Convert to expression body
// ACTION: Make private
// ACTION: Make internal

@J.<caret>foo(1, "2") fun test() {

}