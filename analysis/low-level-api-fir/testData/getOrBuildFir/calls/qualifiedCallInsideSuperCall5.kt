// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtSimpleNameExpression
open class A(init: A.() -> Unit) {
    val prop: String = ""
}

object B : A({})

object C : A({})

class G : A(
    {
        fun foo() = B.prop.toString()
    }
) {
    constructor() : super(
        {
            fun foo() = C.<expr>prop</expr>.toString()
        }
    )
}