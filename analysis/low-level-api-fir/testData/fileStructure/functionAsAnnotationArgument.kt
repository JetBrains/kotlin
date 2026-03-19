/* RootStructureElement */annotation class Anno(val i: Int)/* DeclarationStructureElement *//* ClassDeclarationStructureElement */

@Anno(i = fun foo() = 1)
abstract class Check {/* ClassDeclarationStructureElement */
    abstract var prop: Int/* DeclarationStructureElement */
}
