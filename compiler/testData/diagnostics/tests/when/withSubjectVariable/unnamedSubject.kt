// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// ISSUE: KT-58458

fun box() =
    when (val<!SYNTAX!><!> = <!UNRESOLVED_REFERENCE!>x<!>) {
        in <!UNRESOLVED_REFERENCE!>y<!> -> ""
        else -> ""
    }

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, stringLiteral, whenExpression,
whenWithSubject */
