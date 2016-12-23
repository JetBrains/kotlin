// "Create label foo@" "false"
// ACTION: Convert to expression body
// ERROR: Unresolved reference: @foo

fun test(): Int {
    return@<caret>foo 1
}