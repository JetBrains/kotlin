// APPROXIMATE_TYPE
interface T

fun f<caret>oo() {
    open class A
    class B : A(), T

    return <expr>B()</expr>
}