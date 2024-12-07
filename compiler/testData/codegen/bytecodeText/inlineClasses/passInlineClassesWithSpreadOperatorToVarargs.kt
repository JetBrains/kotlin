// LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(val u: Int)

// FILE: test.kt

fun <T> takeVarargs(vararg e: T) {}

fun test(u1: UInt, u2: UInt, us: Array<UInt>) {
    takeVarargs(*us)
    takeVarargs(u1, u2, *us)
}

// @TestKt.class:
// 2 INVOKESTATIC UInt\.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 CHECKCAST \[Ljava/lang/Integer

// 0 intValue
// 0 valueOf

// 0 CHECKCAST \[LUInt
