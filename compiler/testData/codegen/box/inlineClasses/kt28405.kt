// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class TestUIntArrayW(val x: UIntArray)

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineCharArray(val x: CharArray) {
    override fun toString(): String = x.contentToString()
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class TestInlineCharArrayW(val x: InlineCharArray)

fun box(): String {
    val t1 = TestUIntArrayW(UIntArray(1)).toString()
    if (!t1.startsWith("TestUIntArrayW")) throw AssertionError(t1)

    val t2 = TestInlineCharArrayW(InlineCharArray(charArrayOf('a'))).toString()
    if (!t2.startsWith("TestInlineCharArrayW")) throw AssertionError(t2)

    return "OK"
}
