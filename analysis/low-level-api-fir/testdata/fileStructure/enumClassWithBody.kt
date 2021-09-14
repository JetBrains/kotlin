enum class A {/* NonReanalyzableDeclarationStructureElement */
    X {
        fun localInX() = 1
    },
    Y {
        override fun foo() {}
    },
    Z,

    ;

    open fun foo() {/* ReanalyzableFunctionStructureElement */}
}