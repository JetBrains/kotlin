// "Change 'foo' function return type to 'U'" "true"
trait T
trait U

fun foo() {
    open class A: T
    class B: A(), U

    return <caret>B()
}