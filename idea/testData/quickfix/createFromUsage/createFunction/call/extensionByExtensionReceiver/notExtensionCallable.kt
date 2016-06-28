// "Create extension function 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create function 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
fun bar(b: Boolean) {

}

fun test() {
    bar(<caret>foo(1))
}