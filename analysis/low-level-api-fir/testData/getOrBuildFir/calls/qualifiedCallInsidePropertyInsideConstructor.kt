// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtSimpleNameExpression
class A(
    val i: () -> Unit = {
        fun foo() = B.<expr>prop</expr>.toString()
    }
)
