// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    constructor(x: Int)
}

val x = A<!NO_VALUE_FOR_PARAMETER!>()<!>

/* GENERATED_FIR_TAGS: classDeclaration, propertyDeclaration, secondaryConstructor */
