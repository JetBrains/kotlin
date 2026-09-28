// RUN_PIPELINE_TILL: CODEGEN
// IGNORE_FIR_DIAGNOSTICS
// IGNORE_ERRORS

class C {
    companion <!REDECLARATION!>object<!> {}

    val <!REDECLARATION!>Companion<!> = C
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, objectDeclaration, propertyDeclaration */
