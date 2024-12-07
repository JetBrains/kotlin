open class A(init: A.() -> Unit)/* DeclarationStructureElement */ {/* ClassDeclarationStructureElement */
    val prop: String = ""/* DeclarationStructureElement */
}

object B : A({})/* ClassDeclarationStructureElement */

object C : A(
    {
        fun foo() = B.prop.toString()
    }
)/* ClassDeclarationStructureElement */

class D : A(
    {
        fun foo() = B.prop.toString()
    }
)/* ClassDeclarationStructureElement */

class E()/* DeclarationStructureElement */ : A(
    {
        fun foo() = B.prop.toString()
    }
)/* ClassDeclarationStructureElement */

class F : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* ClassDeclarationStructureElement */
    constructor()/* DeclarationStructureElement */
}

class G : A(
    {
        fun foo() = B.prop.toString()
    }
) {/* ClassDeclarationStructureElement */
    constructor() : super(
        {
            fun foo() = B.prop.toString()
        }
    )/* DeclarationStructureElement */
}

class H : A {/* ClassDeclarationStructureElement */
    constructor() : super(
        {
            fun foo() = B.prop.toString()
        }
    )/* DeclarationStructureElement */
}
