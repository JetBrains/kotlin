// "Create label foo@" "false"
// ACTION: Convert to expression body
// ERROR: The label '@foo' does not denote a loop
// ERROR: Unresolved reference: @foo

fun test() {
    continue@<caret>foo
}