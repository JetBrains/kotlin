enum class A {/* ClassDeclarationStructureElement */
    X {/* DeclarationStructureElement */
        fun localInX() = 1
    },
    Y {/* DeclarationStructureElement */
        override fun foo() {}
    },
    Z,

    ;/* DeclarationStructureElement */

    open fun foo() {/* DeclarationStructureElement */}
}
