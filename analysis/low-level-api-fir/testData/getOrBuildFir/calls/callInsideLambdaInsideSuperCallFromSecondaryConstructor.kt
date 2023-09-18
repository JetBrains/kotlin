// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtCallExpression

open class A(x: () -> Unit)

class B : A {
    constructor(i: Int) : super(
        {
            foo(i)
        }
    )

    constructor(l: Long) : super(
        {
            <expr>foo(l)</expr>
        }
    )
}

fun foo(any: Any) {}