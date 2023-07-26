open class A(x: () -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement *//* NonReanalyzableClassDeclarationStructureElement */

class B : A {/* NonReanalyzableClassDeclarationStructureElement */
    constructor(i: Int) : super(
        {
            foo(i)
        }
    )/* NonReanalyzableNonClassDeclarationStructureElement */
}

fun foo(any: Any) {/* ReanalyzableFunctionStructureElement */}
