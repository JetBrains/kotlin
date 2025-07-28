// APPROXIMATE_TYPE
interfa<caret>ce T

fun foo() {
    open class A : T
    class B : A()

    return <expr>B()</expr>
}