// "Change return type of current function 'foo' to 'T'" "true"
interface T

fun foo() {
    open class A: T
    class B: A()

    return <caret>B()
}