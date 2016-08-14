// "Remove redundant call to 'toString()'" "true"

fun foo(s: String) = s

fun bar() = foo("a${"b".toString()<caret>}")