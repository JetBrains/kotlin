class A {/* NonReanalyzableDeclarationStructureElement */
    val a = run {
        class X()

        val y = 10
    }/* NonReanalyzableDeclarationStructureElement */
}

inline fun <R> run(block: () -> R): R {/* ReanalyzableFunctionStructureElement */
    return block()
}
