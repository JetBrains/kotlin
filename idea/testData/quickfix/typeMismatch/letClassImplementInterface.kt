// "Let 'B' implement interface 'A'" "true"
// WITH_RUNTIME
package let.implement

fun bar() {
    foo(B()<caret>)
}


fun foo(a: A) {
}

interface A {
    fun gav()
}
class B