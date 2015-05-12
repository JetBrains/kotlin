// "Change 'foo' function return type to 'T'" "true"
interface T

fun foo() {
    open class A: T
    class B: A()

    return <caret>B()
}