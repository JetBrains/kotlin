// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

fun foo(s: S) = s

fun box(): String {
    val fooRef = ::foo

    assertEquals("abc", fooRef.invoke(S("abc")).string)
    assertEquals("foo", fooRef.name)

    return "OK"
}