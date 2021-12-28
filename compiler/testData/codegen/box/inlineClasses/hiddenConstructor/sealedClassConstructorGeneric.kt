// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(val string: T)

sealed class Sealed(val x: S<String>)

class Test(x: S<String>) : Sealed(x)

fun box() = Test(S("OK")).x.string