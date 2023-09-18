class B {/* ClassDeclarationStructureElement */
    fun q(): C {/* DeclarationStructureElement */}
    private val y = q()/* DeclarationStructureElement */

    fun foo(a: A) = with(a) {
        bar("a", y)
    }/* DeclarationStructureElement */
}
