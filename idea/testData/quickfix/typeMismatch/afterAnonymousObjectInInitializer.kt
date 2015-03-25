// "Change 't' type to 'T'" "true"
trait T

fun foo() {
    val t: T = object: T{}
}