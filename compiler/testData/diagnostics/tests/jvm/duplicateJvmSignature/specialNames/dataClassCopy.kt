// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

data class <!CONFLICTING_JVM_DECLARATIONS!>C(val c: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>fun `copy$default`(c: C, x: Int, m: Int, mh: Any)<!> = C(this.c)
}

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, primaryConstructor, propertyDeclaration,
thisExpression */
