open class A
    (init: A.() -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement */
{/* NonReanalyzableClassDeclarationStructureElement */
    val prop: String = ""/* ReanalyzablePropertyStructureElement */
}

class B()/* NonReanalyzableNonClassDeclarationStructureElement */ : A()/* NonReanalyzableClassDeclarationStructureElement */

object C/* NonReanalyzableNonClassDeclarationStructureElement */ : A(
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

class D/* NonReanalyzableNonClassDeclarationStructureElement */ : A(
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
