open class B(x: () -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement *//* NonReanalyzableClassDeclarationStructureElement */

class A : B(1, {
    foo()
})/* NonReanalyzableClassDeclarationStructureElement */

fun foo() {/* ReanalyzableFunctionStructureElement */}
