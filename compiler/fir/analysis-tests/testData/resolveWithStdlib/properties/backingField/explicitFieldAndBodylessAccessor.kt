// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80383, KT-81222
// FIR_IDENTICAL

val a : List<String>
    field = mutableListOf<String>()
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get<!>

val b: Int
    <!REDUNDANT_EXPLICIT_BACKING_FIELD!>field<!> = 1
    <!PROPERTY_WITH_EXPLICIT_FIELD_AND_ACCESSORS!>get()<!> = 25

/* GENERATED_FIR_TAGS: propertyDeclaration */
