// APPROXIMATE_TYPE
interface T

fun foo() {
    open class A
    class B : A(), T

    return <expr>B()</expr>
}