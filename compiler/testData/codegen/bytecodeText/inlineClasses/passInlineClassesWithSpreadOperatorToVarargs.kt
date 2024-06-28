// LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(val u: Int)

// FILE: test.kt

fun <T> takeVarargs(vararg e: T) {}

fun test(u1: UInt, u2: UInt, us: Array<UInt>) {
    takeVarargs(*us) // copy + checkcast (on non-ir backend)
    takeVarargs(u1, u2, *us) // 2 box + checkcast (on non-ir backend)
}

// @TestKt.class:
// 2 INVOKESTATIC UInt\.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 CHECKCAST \[Ljava/lang/Integer

// 0 intValue
// 0 valueOf

// JVM_TEMPLATES
// 2 CHECKCAST \[LUInt

// JVM_IR_TEMPLATES
// 0 CHECKCAST \[LUInt
