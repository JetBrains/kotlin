// RUN_PIPELINE_TILL: FRONTEND
// SKIP_TXT
// ISSUE: KT-8263

fun test(x: Int, y: Int) {
    if (<!CONDITION_TYPE_MISMATCH!><!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>x<!> < (<!SYNTAX!>if<!><!SYNTAX!><!> <!SYNTAX!>(<!><!UNRESOLVED_REFERENCE!><!SYNTAX!><!>y<!> ><!><!SYNTAX!><!> 115<!SYNTAX!>) 1 else 2))<!> {
        Unit
    }
}
