// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80383
// FIR_IDENTICAL

val a : List<String>
    field = mutableListOf<String>()
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get<!>

/* GENERATED_FIR_TAGS: propertyDeclaration */
