// APPROXIMATE_TYPE
interface T

fun foo() {
    open class A
    class B : A()

    return <expr>B()</expr>
}