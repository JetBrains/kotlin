// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

abstract class Base(val x: S)

class Test(x: S) : Base(x)

fun box() = Test(S("OK")).x.string