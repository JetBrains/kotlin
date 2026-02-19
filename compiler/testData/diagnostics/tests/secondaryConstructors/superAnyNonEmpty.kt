// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(): super(<!TOO_MANY_ARGUMENTS!>1<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, secondaryConstructor */
