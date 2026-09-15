// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -RichErrors

<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_TARGET!>error<!> class Foo
<!UNSUPPORTED_FEATURE, WRONG_MODIFIER_TARGET!>error<!> object Bar

fun foo(x: <!UNSUPPORTED_FEATURE!>String | Foo<!>) {
    <!UNNECESSARY_SAFE_CALL!>x<!UNSUPPORTED_FEATURE!>|.<!>length<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, objectDeclaration, safeCall */
