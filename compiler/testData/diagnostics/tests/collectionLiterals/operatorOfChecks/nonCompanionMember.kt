// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class Klass {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member of companion")!>operator<!> fun of(vararg t: Any): Klass = Klass()
}

object Obj {
    <!INAPPLICABLE_OPERATOR_MODIFIER("must be a member of companion")!>operator<!> fun of(vararg t: Any): Obj = Obj
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, operator, vararg */
