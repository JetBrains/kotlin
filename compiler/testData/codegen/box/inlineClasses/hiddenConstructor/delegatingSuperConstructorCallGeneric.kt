// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

abstract class Base(val x: S<String>)

class Test(x: S<String>) : Base(x)

fun box() = Test(S("OK")).x.string