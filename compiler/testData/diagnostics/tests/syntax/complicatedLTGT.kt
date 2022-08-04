// SKIP_TXT
// ISSUE: KT-8263

fun test(x: Int, y: Int) {
    if (<!UNRESOLVED_REFERENCE!>x<!> < (<!SYNTAX!>if<!><!SYNTAX!><!> <!SYNTAX!>(<!><!UNRESOLVED_REFERENCE!><!SYNTAX!><!>y<!> ><!SYNTAX!><!> 115<!SYNTAX!>) 1 else 2))<!> {
        Unit
    }
}
