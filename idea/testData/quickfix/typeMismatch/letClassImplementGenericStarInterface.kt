// "Let 'B' implement interface 'A<*>'" "false"
// ACTION: Change parameter 'a' type of function 'let.implement.foo' to 'B'
// ACTION: Convert to expression body
// ACTION: Create function 'foo'
// ERROR: Type mismatch: inferred type is B but A<*> was expected

package let.implement

fun bar() {
    foo(B()<caret>)
}


fun foo(a: A<*>) {
}

interface A<T>
class B