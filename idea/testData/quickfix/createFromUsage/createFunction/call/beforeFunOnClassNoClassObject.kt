// "Create function 'foo' from usage" "false"
// ACTION: Replace with infix function call
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

class A<T>(val n: T)

fun test() {
    val a: Int = A.<caret>foo(2)
}