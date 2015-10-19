// "Create member property 'foo' as constructor parameter" "false"
// ACTION: Create member property 'foo'
// ERROR: Unresolved reference: foo

fun test() {
    val a: Int = J.<caret>foo
}

