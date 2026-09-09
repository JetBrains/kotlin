/* RootStructureElement *//* RootScriptStructureElement */class A {/* ClassDeclarationStructureElement */
    val member: Any/* DeclarationStructureElement */
        field: Int = run {
            fun local() {}
            1
        }
}

val topLevel: Any/* DeclarationStructureElement */
    field: Int = 1
    get() = field
