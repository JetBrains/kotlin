// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

abstract class Base(val x: S)

class Test : Base {
    constructor() : super(S("OK"))
}

fun box() = Test().x.string