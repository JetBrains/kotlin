// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// ISSUE: KT-8263

fun test(x: Int, y: Int) {
    if (<!CONDITION_TYPE_MISMATCH!>x <!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>< (<!SYNTAX!>if<!><!SYNTAX!><!> <!SYNTAX!>(<!><!UNRESOLVED_REFERENCE!><!SYNTAX!><!>y<!> ><!><!><!SYNTAX!><!> 115<!SYNTAX!>) 1 else 2))<!> {
        Unit
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, integerLiteral, lambdaLiteral */
