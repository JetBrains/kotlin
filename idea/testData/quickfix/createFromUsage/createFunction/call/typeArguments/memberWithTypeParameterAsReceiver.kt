// "Create member function 'bar'" "false"
// ACTION: Create extension function 'T.bar'
// ACTION: Rename reference
// ERROR: Unresolved reference: bar
fun <T> foo(t: T) {
    t.<caret>bar()
}