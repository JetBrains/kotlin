// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create extension property 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2.<caret>foo
}
