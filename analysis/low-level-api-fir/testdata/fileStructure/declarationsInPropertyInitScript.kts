/* RootScriptStructureElement */class A {/* NonReanalyzableClassDeclarationStructureElement */
    val a = run {
        class X()

        val y = 10
    }/* NonReanalyzableNonClassDeclarationStructureElement */
}

inline fun <R> run(block: () -> R): R {/* ReanalyzableFunctionStructureElement */
    return block()
}
