// "Create label foo@" "false"
// ERROR: The label '@foo' does not denote a loop
// ERROR: Unresolved reference: @foo

fun test() {
    break@<caret>foo
}