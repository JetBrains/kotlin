// "Let 'String' implement interface 'A'" "false"
// ACTION: Change parameter 'a' type of function 'let.implement.foo' to 'String'
// ACTION: Convert to expression body
// ACTION: To raw string literal
// ACTION: Create function 'foo'
// ERROR: Type mismatch: inferred type is String but A was expected

package let.implement

fun bar() {
    foo("Hello"<caret>)
}


fun foo(a: A) {
}

interface A