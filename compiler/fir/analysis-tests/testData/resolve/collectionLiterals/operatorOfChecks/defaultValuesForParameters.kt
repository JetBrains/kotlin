// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class DefaultVararg {
    companion object {
        <!INAPPLICABLE_OPERATOR_MODIFIER("should not have parameters with default values")!>operator<!> fun of(vararg def: Int = intArrayOf(1, 2, 3)): DefaultVararg = DefaultVararg()
    }
}

class DefaultRegular {
    companion object {
        operator fun of(vararg notDef: Int): DefaultRegular = DefaultRegular()
        <!INAPPLICABLE_OPERATOR_MODIFIER("should not have parameters with default values")!>operator<!> fun of(def: Int = 42): DefaultRegular = DefaultRegular()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
operator, vararg */
