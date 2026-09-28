// RUN_PIPELINE_TILL: CODEGEN
val it: Int
    <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!> = 42

/* GENERATED_FIR_TAGS: explicitBackingField, integerLiteral, propertyDeclaration */
