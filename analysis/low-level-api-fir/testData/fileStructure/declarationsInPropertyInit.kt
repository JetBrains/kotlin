class A {/* ClassDeclarationStructureElement */
    val a = myRun {
        class X()

        val y = 10
    }/* DeclarationStructureElement */
}

inline fun <R> myRun(block: () -> R): R {/* DeclarationStructureElement */
    return block()
}
