// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

fun test(n: Int) {
    <caret>foo = n + 1
}