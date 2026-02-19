// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-80535

val e : Any
    field: Int <!FIELD_INITIALIZER_TYPE_MISMATCH!>=<!> ""

/* GENERATED_FIR_TAGS: explicitBackingField, propertyDeclaration, stringLiteral */
