// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -RichErrors
// WITH_STDLIB
// FILE: test.kt

<!UNSUPPORTED_FEATURE!>error<!> class Foo
<!UNSUPPORTED_FEATURE!>error<!> object Bar

fun <T : <!UNSUPPORTED_FEATURE!>RichError<!>, V : <!UNRESOLVED_REFERENCE!>NonError<!>> foo(
    x: <!UNSUPPORTED_FEATURE!>String | Foo<!>,
    re: <!UNSUPPORTED_FEATURE!>RichError<!>,
    v: <!UNRESOLVED_REFERENCE!>NonError<!>,
) {
    x<!UNSUPPORTED_FEATURE!>|.<!>length
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>RichError<!>::class.java<!>
    <!UNRESOLVED_REFERENCE!>NonError<!>::class.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>java<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, objectDeclaration, safeCall */
