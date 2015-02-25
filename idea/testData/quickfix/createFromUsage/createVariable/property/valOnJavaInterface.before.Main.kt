// "Create property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Disable 'Convert to Expression Body'
// ACTION: Edit intention settings
// ACTION: Create extension property 'foo'
// ERROR: Unresolved reference: foo

fun test(a: A): String? {
    return a.<caret>foo
}