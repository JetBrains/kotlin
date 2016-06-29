// "Create property 'foo' as constructor parameter" "false"
// ACTION: Create extension property 'Int.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
// WITH_RUNTIME

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2.<caret>foo
}
