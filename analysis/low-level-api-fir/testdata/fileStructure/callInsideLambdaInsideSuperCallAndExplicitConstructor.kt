open class B(x: () -> Unit)/* NonReanalyzableNonClassDeclarationStructureElement *//* NonReanalyzableClassDeclarationStructureElement */

class A()/* NonReanalyzableNonClassDeclarationStructureElement */ : B(1, {
    foo()
})/* NonReanalyzableClassDeclarationStructureElement */

fun foo() {/* ReanalyzableFunctionStructureElement */}
