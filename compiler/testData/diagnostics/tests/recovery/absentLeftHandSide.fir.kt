// !WITH_NEW_INFERENCE
fun import() {
    <!UNRESOLVED_REFERENCE!>import<!> <!UNRESOLVED_REFERENCE!>a<!><!SYNTAX!>.<!><!UNRESOLVED_REFERENCE!>*<!><!SYNTAX!><!>
}

fun composite() {
    val s = 13<!AMBIGUITY!>+<!><!SYNTAX!>~<!><!UNRESOLVED_REFERENCE!>/<!>12
}

fun html() {
    <!SYNTAX!><<!><!UNRESOLVED_REFERENCE!>html<!>><!SYNTAX!><<!><!UNRESOLVED_REFERENCE!>/<!><!UNRESOLVED_REFERENCE!>html<!>><!SYNTAX!><!>
}

fun html1() {
    <!SYNTAX!><<!><!UNRESOLVED_REFERENCE!>html<!>><!SYNTAX!><<!><!UNRESOLVED_REFERENCE!>/<!><!UNRESOLVED_REFERENCE!>html<!>><!UNRESOLVED_REFERENCE!>html<!>
}