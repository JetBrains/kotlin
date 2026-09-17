// LL_FIR_DIVERGENCE
//   Value is available in AA even when the LF is disabled
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -RichErrors
// WITH_STDLIB

<!UNSUPPORTED_FEATURE!>error<!> class Foo
<!UNSUPPORTED_FEATURE!>error<!> object Bar

fun <T : <!UNSUPPORTED_FEATURE!>RichError<!>, V : <!UNSUPPORTED_FEATURE!>Value<!>> foo(
    x: <!UNSUPPORTED_FEATURE!>String | Foo<!>,
    re: <!UNSUPPORTED_FEATURE!>RichError<!>,
    v: <!UNSUPPORTED_FEATURE!>Value<!>,
) {
    <!UNNECESSARY_SAFE_CALL!>x<!UNSUPPORTED_FEATURE!>|.<!>length<!>
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>RichError<!>::class.java<!>
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>Value<!>::class.java<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, objectDeclaration, safeCall */
