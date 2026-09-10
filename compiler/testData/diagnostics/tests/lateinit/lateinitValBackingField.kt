// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +LateinitVals

val foo: CharSequence
    <!WRONG_MODIFIER_TARGET!>lateinit<!> field: String

/* GENERATED_FIR_TAGS: explicitBackingField, propertyDeclaration */
