enum class A {/* NonReanalyzableDeclarationStructureElement */
    X,/* NonReanalyzableDeclarationStructureElement */
    Y,/* NonReanalyzableDeclarationStructureElement */
    Z

    ;/* NonReanalyzableDeclarationStructureElement */

    fun foo(){/* ReanalyzableFunctionStructureElement */}

    val x = 10/* NonReanalyzableDeclarationStructureElement */

    fun bar() = 10/* NonReanalyzableDeclarationStructureElement */
}
