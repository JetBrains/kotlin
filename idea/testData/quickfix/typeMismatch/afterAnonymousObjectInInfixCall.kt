// "Change parameter 't' type of function 'foo' to 'T'" "true"
trait T

fun Int.foo(t: T) = this

fun foo() {
    1 foo object: T{}
}