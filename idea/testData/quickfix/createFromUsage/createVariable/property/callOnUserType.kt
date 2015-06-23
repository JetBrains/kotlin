// "Create property 'foo'" "false"
// ACTION: Create extension function 'bar'
// ACTION: Create member function 'bar'
// ACTION: Replace with infix function call
// ERROR: Unresolved reference: bar
// ERROR: Unresolved reference: foo

class A<T>(val n: T) {
    val foo: Int = 1
}

fun test() {
    A(1).<caret>bar(foo)
}
