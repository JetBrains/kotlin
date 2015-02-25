// "Create property 'foo'" "true"
// ERROR: Unresolved reference: foo

fun test(): String? {
    return A().<caret>foo
}