open class A(x: () -> Unit)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

class B : A {/* ClassDeclarationStructureElement */
    constructor(i: Int) : super(
        {
            foo(i)
        }
    )/* DeclarationStructureElement */
}

fun foo(any: Any) {/* DeclarationStructureElement */}
