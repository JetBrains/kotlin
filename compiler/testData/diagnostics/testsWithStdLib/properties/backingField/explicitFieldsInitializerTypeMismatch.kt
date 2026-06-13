// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80535

val e : Any
    field: Int <!FIELD_INITIALIZER_TYPE_MISMATCH!>=<!> ""

/* GENERATED_FIR_TAGS: explicitBackingField, propertyDeclaration, stringLiteral */
