// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2.<caret>foo
}
