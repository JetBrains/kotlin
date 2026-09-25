// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -RichErrors
// WITH_STDLIB

<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_TARGET!>error<!> class Foo
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_TARGET!>error<!> object Bar

fun <T : <!UNSUPPORTED_FEATURE!>RichError<!>, V : <!UNRESOLVED_REFERENCE!>Value<!>> foo(
    x: <!UNSUPPORTED_FEATURE!>String | Foo<!>,
    re: <!UNSUPPORTED_FEATURE!>RichError<!>,
    v: <!UNRESOLVED_REFERENCE!>Value<!>,
) {
    x<!UNNECESSARY_SAFE_CALL, UNSUPPORTED_FEATURE!>|.<!>length
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>RichError<!>::class.java<!>
    <!UNRESOLVED_REFERENCE!>Value<!>::class.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>java<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, objectDeclaration, safeCall */
