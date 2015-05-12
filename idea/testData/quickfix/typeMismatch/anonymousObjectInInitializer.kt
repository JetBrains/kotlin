// "Change 't' type to 'T'" "true"
interface T

fun foo() {
    val t: Int = <caret>object: T{}
}