// APPROXIMATE_TYPE
interface T

fun f<caret>oo() {
    open class A
    class B : A()

    return <expr>B()</expr>
}