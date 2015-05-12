// "Change 'foo' function return type to 'T'" "true"
interface T

fun foo() {
    return <caret>object: T{}
}