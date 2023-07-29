/* RootScriptStructureElement */enum class A {/* NonReanalyzableClassDeclarationStructureElement */
    X {/* NonReanalyzableNonClassDeclarationStructureElement */
        fun localInX() = 1
    },
    Y {/* NonReanalyzableNonClassDeclarationStructureElement */
        override fun foo() {}
    },
    Z,

    ;/* NonReanalyzableNonClassDeclarationStructureElement */

    open fun foo() {/* ReanalyzableFunctionStructureElement */}
}
