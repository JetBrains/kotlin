/* RootScriptStructureElement */class A {/* ClassDeclarationStructureElement */
    val a = run {
        class X()

        val y = 10
    }/* DeclarationStructureElement */
}

inline fun <R> run(block: () -> R): R {/* DeclarationStructureElement */
    return block()
}
