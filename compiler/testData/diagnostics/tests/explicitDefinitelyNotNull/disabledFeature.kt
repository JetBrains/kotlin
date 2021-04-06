// !LANGUAGE: -DefinitelyNotNullTypeParameters

fun <T> foo(x: T, y: <!UNSUPPORTED_FEATURE!>T!!<!>): List<<!UNSUPPORTED_FEATURE!>T!!<!>>? = null
