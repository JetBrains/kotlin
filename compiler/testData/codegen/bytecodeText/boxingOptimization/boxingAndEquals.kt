// !LANGUAGE: +InlineClasses

// https://youtrack.jetbrains.com/issue/KT-15871

// FILE: Test.kt

fun getAndCheckInt(a: Int, b: Int) =
        getAndCheck({ a }, { b })

// @TestKt.class:
// 0 valueOf
// 0 Value
// 0 areEqual

// FILE: TestInlined.kt

fun getAndCheckInlinedInt(a: InlinedInt, b: InlinedInt) =
        getAndCheck({ a }, { b })

// @TestInlinedKt.class:
// 0 valueOf
// 0 Value
// 0 areEqual
// 0 INVOKESTATIC InlinedInt\$Erased.box
// 0 INVOKEVIRTUAL InlinedInt.unbox

// FILE: Inline.kt
inline fun <T> getAndCheck(getFirst: () -> T, getSecond: () -> T) =
        getFirst() == getSecond()

inline class InlinedInt(val x: Int)
