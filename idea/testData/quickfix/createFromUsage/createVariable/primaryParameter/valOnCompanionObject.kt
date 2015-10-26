// "Create member property 'foo' as constructor parameter" "false"
// ACTION: Create member property 'foo'
// ACTION: Create extension property 'foo'
// ERROR: Unresolved reference: foo

class A<T>(val n: T) {
    companion object {

    }
}

fun test() {
    val a: Int = A.<caret>foo
}
