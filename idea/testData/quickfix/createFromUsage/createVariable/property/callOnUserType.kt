// "Create property 'foo'" "false"
// ACTION: Create extension function 'A<Int>.bar'
// ACTION: Create member function 'A.bar'
// ACTION: Rename reference
// ACTION: Convert to run
// ACTION: Convert to with
// ERROR: Unresolved reference: bar
// ERROR: Unresolved reference: foo

class A<T>(val n: T) {
    val foo: Int = 1
}

fun test() {
    A(1).<caret>bar(foo)
}
