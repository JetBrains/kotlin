// https://youtrack.jetbrains.com/issue/KT-15871

// FILE: Test.kt

fun getAndCheckInt(a: Int, b: Int) =
        getAndCheck({ a }, { b })

// @TestKt.class:
// 0 valueOf
// 0 Value
// 0 areEqual

// FILE: Inline.kt
inline fun <T> getAndCheck(getFirst: () -> T, getSecond: () -> T) =
        getFirst() == getSecond()


