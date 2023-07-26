open class A(init: A.() -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement */ {/* NonReanalyzableClassDeclarationStructureElement */
    val prop: String = ""/* NonReanalyzableNonClassDeclarationStructureElement */
}

object B : A({})/* NonReanalyzableClassDeclarationStructureElement */

object C : A(
    {
        fun foo() = B.prop.toString()
    }
)/* NonReanalyzableClassDeclarationStructureElement */

class D : A(
    {
        fun foo() = B.prop.toString()
    }
)/* NonReanalyzableClassDeclarationStructureElement */

class E()/* NonReanalyzableNonClassDeclarationStructureElement */ : A(
    {
        fun foo() = B.prop.toString()
    }
)/* NonReanalyzableClassDeclarationStructureElement */

class F : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* NonReanalyzableClassDeclarationStructureElement */
    constructor()/* NonReanalyzableNonClassDeclarationStructureElement */
}

class G : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* NonReanalyzableClassDeclarationStructureElement */
    constructor() : super(
        {
            fun foo() = B.prop.toString()
        }
    )/* NonReanalyzableNonClassDeclarationStructureElement */
}

class H : A {/* NonReanalyzableClassDeclarationStructureElement */
    constructor() : super(
        {
            fun foo() = B.prop.toString()
        }
    )/* NonReanalyzableNonClassDeclarationStructureElement */
}
