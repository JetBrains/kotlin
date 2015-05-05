// WITH_RUNTIME
package foo.kotlin

fun foo() {
    if <caret>(true) {
        throw AssertionError("text")
    }
}

fun assert(x: Boolean, y: Any) {}