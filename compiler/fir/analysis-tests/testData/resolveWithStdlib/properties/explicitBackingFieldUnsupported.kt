// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ExplicitBackingFields

val that: Number
    <!UNSUPPORTED_FEATURE!>field = 4<!>

/* GENERATED_FIR_TAGS: explicitBackingField, integerLiteral, propertyDeclaration */
