// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

var x: Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>10<!>
    get() { return <!RETURN_TYPE_MISMATCH!>""<!> }
    set(value) { return <!RETURN_TYPE_MISMATCH!>20<!> }

/* GENERATED_FIR_TAGS: assignment, getter, propertyDeclaration, setter, stringLiteral */
