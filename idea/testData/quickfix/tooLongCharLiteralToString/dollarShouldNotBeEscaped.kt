// "Convert too long character literal to string" "true"
// ERROR: Unresolved reference: bar

fun foo() {
    'foo$bar'<caret>
}