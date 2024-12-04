// APPROXIMATE_TYPE
interface T

fun foo() {
    open class A : T
    class B : A()

    return <expr>B()</expr>
}