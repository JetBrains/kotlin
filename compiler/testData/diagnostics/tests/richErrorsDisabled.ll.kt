// LL_FIR_DIVERGENCE
//   Value is available in AA even when the LF is disabled
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -RichErrors
// WITH_STDLIB
// ALLOW_KOTLIN_PACKAGE
// FILE: RichError.kt
package kotlin

abstract class RichError
// FILE: test.kt

<!UNSUPPORTED_FEATURE!>error<!> class Foo
<!UNSUPPORTED_FEATURE!>error<!> object Bar

fun <T : <!UNSUPPORTED_FEATURE!>RichError<!>, V : <!UNSUPPORTED_FEATURE!>NonError<!>> foo(
    x: <!UNSUPPORTED_FEATURE!>String | Foo<!>,
    re: <!UNSUPPORTED_FEATURE!>RichError<!>,
    v: <!UNSUPPORTED_FEATURE!>NonError<!>,
) {
    x<!UNNECESSARY_SAFE_CALL, UNSUPPORTED_FEATURE!>|.<!>length
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>RichError<!>::class.java<!>
    <!UNSUPPORTED_FEATURE!><!UNSUPPORTED_FEATURE!>NonError<!>::class.java<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, objectDeclaration, safeCall */
