// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    "a"."b"::<!UNRESOLVED_REFERENCE!>foo<!>
    "a"."b"::class
    "a"."b"."c"::<!UNRESOLVED_REFERENCE!>foo<!>
    "a"."b"."c"::class
}
