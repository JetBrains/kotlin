enum class A {/* NonReanalyzableDeclarationStructureElement */
    X,
    Y,
    Z

    ;

    fun foo(){/* ReanalyzableFunctionStructureElement */}

    val x = 10/* NonReanalyzableDeclarationStructureElement */

    fun bar() = 10/* NonReanalyzableDeclarationStructureElement */
}