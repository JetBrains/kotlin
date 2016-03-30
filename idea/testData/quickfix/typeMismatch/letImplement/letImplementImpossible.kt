// "Let 'B' implement interface 'A<Y>'" "false"
// ACTION: Change parameter 'a' type of function 'let.implement.foo' to 'B'
// ACTION: Convert to expression body
// ERROR: Type mismatch: inferred type is B but A<Y> was expected
package let.implement

fun main(args: Array<String>) {
    foo(B()<caret>)
}


fun foo(a: A<Y>) {
}

interface A<T>
open class C : A<X>
class B : C()

class X
class Y