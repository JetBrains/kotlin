// "Convert to block body" "true"
// ERROR: Unresolved reference: bar
fun <caret>foo(): String {
    return bar()
}