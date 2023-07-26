class B {/* NonReanalyzableClassDeclarationStructureElement */
    fun q(): C {/* ReanalyzableFunctionStructureElement */}
    private val y = q()/* NonReanalyzableNonClassDeclarationStructureElement */

    fun foo(a: A) = with(a) {
        bar("a", y)
    }/* NonReanalyzableNonClassDeclarationStructureElement */
}
