// "Change 'foo' function return type to 'T'" "true"
trait T

fun foo() {
    return <caret>object: T{}
}