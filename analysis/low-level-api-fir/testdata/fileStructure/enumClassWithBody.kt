enum class A {/* NonReanalyzableDeclarationStructureElement */
    X {/* NonReanalyzableDeclarationStructureElement */
        fun localInX() = 1
    },
    Y {/* NonReanalyzableDeclarationStructureElement */
        override fun foo() {}
    },
    Z,

    ;/* NonReanalyzableDeclarationStructureElement */

    open fun foo() {/* ReanalyzableFunctionStructureElement */}
}
