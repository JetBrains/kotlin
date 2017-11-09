// "Create class 'Foo'" "false"
// ACTION: Create function 'Foo'
// ACTION: Remove parameter 's'
// ACTION: Create secondary constructor
// ACTION: Add 'i =' to argument
// ERROR: No value passed for parameter 's'

class Foo(i: Int, s: String)

fun test() {
    val a = Foo(2<caret>)
}