// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtCallExpression
open class A(init: A.() -> Unit) {
    val prop: String = ""
}

object B : A({})

object C : A(
    {
        fun foo() = B.prop.<expr>toString()</expr>
    }
)