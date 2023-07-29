/* RootScriptStructureElement */open class A
    (init: A.() -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement */
{/* NonReanalyzableClassDeclarationStructureElement */
    val prop: String = ""/* NonReanalyzableNonClassDeclarationStructureElement */
}

class B()/* NonReanalyzableNonClassDeclarationStructureElement */ : A()/* NonReanalyzableClassDeclarationStructureElement */

object C : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* NonReanalyzableClassDeclarationStructureElement */

}

val f = object : A(
    {
        fun bar() = B.prop.toString()
    }
) {

}/* NonReanalyzableNonClassDeclarationStructureElement */

class D : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* NonReanalyzableClassDeclarationStructureElement */
    constructor(): super(
        {
            fun boo() = prop.toString()
        }
    )/* NonReanalyzableNonClassDeclarationStructureElement */
}
