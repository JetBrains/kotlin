// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

import kotlin.test.*

inline class TestUIntArrayW(val x: UIntArray)

inline class InlineCharArray(val x: CharArray) {
    override fun toString(): String = x.contentToString()
}

inline class TestInlineCharArrayW(val x: InlineCharArray)

fun box(): String {
    val t1 = TestUIntArrayW(UIntArray(1)).toString()
    if (!t1.startsWith("TestUIntArrayW")) throw AssertionError(t1)

    val t2 = TestInlineCharArrayW(InlineCharArray(charArrayOf('a'))).toString()
    if (!t2.startsWith("TestInlineCharArrayW")) throw AssertionError(t2)

    return "OK"
}
