// "Create property 'foo'" "false"
// ERROR: Unresolved reference: @foo
fun refer() {
    val v = this@<caret>foo
}