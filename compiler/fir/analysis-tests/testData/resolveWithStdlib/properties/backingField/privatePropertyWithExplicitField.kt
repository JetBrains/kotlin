// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-80445

<!EXPLICIT_FIELD_VISIBILITY_MUST_BE_LESS_PERMISSIVE!>private<!> val b: List<String>
    field = mutableListOf()

/* GENERATED_FIR_TAGS: propertyDeclaration */
