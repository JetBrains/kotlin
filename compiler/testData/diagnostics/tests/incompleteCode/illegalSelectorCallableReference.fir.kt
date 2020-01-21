// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    <!UNRESOLVED_REFERENCE!>"a"."b"::foo<!>
    "a"."b"::class
    <!UNRESOLVED_REFERENCE!>"a"."b"."c"::foo<!>
    "a"."b"."c"::class
}
