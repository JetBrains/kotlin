// "Create property 'foo' from usage" "false"
// ACTION: Create function 'bar' from usage
// ACTION: Replace with infix function call
// ERROR: Unresolved reference: bar
// ERROR: Unresolved reference: foo

class A<T>(val n: T) {
    val foo: Int = 1
}

fun test() {
    A(1).<caret>bar(foo)
}
