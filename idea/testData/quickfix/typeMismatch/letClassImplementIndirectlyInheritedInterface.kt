// "Let 'B' implement interface 'A'" "false"
// ACTION: Add 'a =' to argument
// ACTION: Convert to expression body
package let.implement

fun bar() {
    foo(B()<caret>)
}


fun foo(a: A) {
}

interface A
interface InBetween : A
class B : InBetween
