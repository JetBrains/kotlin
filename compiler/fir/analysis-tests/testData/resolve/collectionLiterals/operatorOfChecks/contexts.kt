// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals, +ContextParameters
// RENDER_DIAGNOSTIC_ARGUMENTS

class C {
    companion object {
        context(ctx: String)
        <!INAPPLICABLE_OPERATOR_MODIFIER("must not have context parameters")!>operator<!> fun of(vararg vs: Int): C = C()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, functionDeclarationWithContext,
objectDeclaration, operator, vararg */
