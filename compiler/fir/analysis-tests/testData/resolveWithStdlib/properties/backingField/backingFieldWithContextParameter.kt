// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

<!CONTEXT_PARAMETERS_WITH_BACKING_FIELD!>context<!>(a: String)
val b: Any
    field = 1

/* GENERATED_FIR_TAGS: explicitBackingField, propertyDeclaration, propertyDeclarationWithContext */
