class A {/* NonReanalyzableClassDeclarationStructureElement */
    val a = myRun {
        class X()

        val y = 10
    }/* NonReanalyzableNonClassDeclarationStructureElement */
}

inline fun <R> myRun(block: () -> R): R {/* ReanalyzableFunctionStructureElement */
    return block()
}
