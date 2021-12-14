// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

class Test(val s: S)

fun box() = Test(S("OK")).s.string