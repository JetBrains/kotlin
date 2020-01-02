// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: A.java
public interface A {
    String foo();
}

// FILE: main.kt

interface B {
    fun foo(): String?
}

interface C {
    fun foo(): String
}

fun foo(x: Any?) {
    if (x is A && x is B) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is B && x is A) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is A && x is C) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is C && x is A) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is A && x is B && x is C) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is B && x is A && x is C) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }

    if (x is B && x is C && x is A) {
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String>() }
        x.<!AMBIGUITY!>foo<!>().<!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><String?>() }
    }
}
