// "Change 'foo' function return type to 'T'" "true"
trait T

fun foo(): T {
    open class A: T
    class B: A()

    return B()
}