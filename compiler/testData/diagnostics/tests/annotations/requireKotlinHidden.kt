// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE
// MODULE: m1
package kotlin.io
@Suppress(<!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)
@kotlin.internal.RequireKotlin("9.9.9", level = DeprecationLevel.HIDDEN)
class Foo

// MODULE: m2(m1)
package kotlin.io

fun foo(f: <!UNRESOLVED_REFERENCE!>Foo<!>) {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, stringLiteral */
