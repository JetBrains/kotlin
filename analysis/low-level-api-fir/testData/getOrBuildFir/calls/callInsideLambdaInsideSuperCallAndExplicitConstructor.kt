// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtCallExpression

open class B(x: () -> Unit)

class A(val f: String = "${42}") : B(1, {
    <expr>foo()</expr>
})

fun foo() {}