// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun test() {
    "a"."b"::foo
    "a"."b"::class
    "a"."b"."c"::foo
    "a"."b"."c"::class
}
