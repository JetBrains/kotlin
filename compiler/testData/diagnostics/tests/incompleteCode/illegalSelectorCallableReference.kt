// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    "a".<!ILLEGAL_SELECTOR!>"b"<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
    "a".<!ILLEGAL_SELECTOR!>"b"<!>::class
    "a".<!ILLEGAL_SELECTOR!>"b"<!>.<!ILLEGAL_SELECTOR!>"c"<!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
    "a".<!ILLEGAL_SELECTOR!>"b"<!>.<!ILLEGAL_SELECTOR!>"c"<!>::class
}
