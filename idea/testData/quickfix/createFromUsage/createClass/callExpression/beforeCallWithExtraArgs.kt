// "Create class 'Foo'" "false"
// ACTION: Create function 'Foo'
// ACTION: Add parameter to constructor 'Foo'
// ACTION: Split property declaration
// ERROR: Too many arguments for public constructor Foo(a: kotlin.Int) defined in Foo

class Foo(a: Int)

fun test() {
    val a = Foo(2, <caret>"2")
}