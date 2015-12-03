// "Create annotation 'foo'" "false"
// ACTION: Create function 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

fun test() = <caret>foo(1, "2")