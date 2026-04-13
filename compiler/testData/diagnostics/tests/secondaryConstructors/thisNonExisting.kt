// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    constructor(x: Int) {}
    constructor(x: String) {}
    constructor(): <!NONE_APPLICABLE!>this<!>('a') {}
}

/* GENERATED_FIR_TAGS: classDeclaration, secondaryConstructor */
