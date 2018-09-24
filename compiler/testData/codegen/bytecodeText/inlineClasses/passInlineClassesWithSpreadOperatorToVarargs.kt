// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(val u: Int)

// FILE: test.kt

fun <T> takeVarargs(vararg e: T) {}

fun test(u1: UInt, u2: UInt, us: Array<UInt>) {
    takeVarargs(*us) // copy + checkcast
    takeVarargs(u1, u2, *us) // 2 box + ...
}

// @TestKt.class:
// 2 INVOKESTATIC UInt\.box
// 0 INVOKEVIRTUAL UInt.unbox

// 2 CHECKCAST \[LUInt

// 0 CHECKCAST \[Ljava/lang/Integer

// 0 intValue
// 0 valueOf