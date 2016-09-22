// "Change return type of enclosing function 'foo' to 'T'" "true"
interface T

fun foo() {
    return <caret>object: T{}
}