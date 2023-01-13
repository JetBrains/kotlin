// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    "a".<!ILLEGAL_SELECTOR!>"b"<!>::<!UNRESOLVED_REFERENCE!>foo<!>
    "a".<!ILLEGAL_SELECTOR!>"b"<!>::class
    "a".<!ILLEGAL_SELECTOR!>"b"<!>.<!ILLEGAL_SELECTOR!>"c"<!>::<!UNRESOLVED_REFERENCE!>foo<!>
    "a".<!ILLEGAL_SELECTOR!>"b"<!>.<!ILLEGAL_SELECTOR!>"c"<!>::class
}
