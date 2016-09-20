// "Change return type of current function 'foo' to 'T'" "true"
interface T

fun foo() {
    return <caret>object: T{}
}