// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_RUNTIME

import kotlin.test.*

@JvmInline
value class TestUIntArrayW(val x: UIntArray)

@JvmInline
value class InlineCharArray(val x: CharArray) {
    override fun toString(): String = x.contentToString()
}

@JvmInline
value class TestInlineCharArrayW(val x: InlineCharArray)

fun box(): String {
    val t1 = TestUIntArrayW(UIntArray(1)).toString()
    if (!t1.startsWith("TestUIntArrayW")) throw AssertionError(t1)

    val t2 = TestInlineCharArrayW(InlineCharArray(charArrayOf('a'))).toString()
    if (!t2.startsWith("TestInlineCharArrayW")) throw AssertionError(t2)

    return "OK"
}
