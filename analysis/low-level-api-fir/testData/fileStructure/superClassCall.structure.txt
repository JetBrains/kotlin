open class A
    (init: A.() -> Unit)/* DeclarationStructureElement */
{/* ClassDeclarationStructureElement */
    val prop: String = ""/* DeclarationStructureElement */
}

class B()/* DeclarationStructureElement */ : A()/* ClassDeclarationStructureElement */

object C : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* ClassDeclarationStructureElement */

}

val f = object : A(
    {
        fun bar() = B.prop.toString()
    }
) {

}/* DeclarationStructureElement */

class D : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* ClassDeclarationStructureElement */
    constructor(): super(
        {
            fun boo() = prop.toString()
        }
    )/* DeclarationStructureElement */
}
