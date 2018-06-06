// !LANGUAGE: +InlineClasses

inline class UInt(val u: Int)

fun <T> takeVarargs(vararg e: T) {}

fun test(u1: UInt, u2: UInt, us: Array<UInt>) {
    takeVarargs(*us) // copy + checkcast
    takeVarargs(u1, u2, *us) // 2 box + ...
}

// 2 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 2 CHECKCAST \[LUInt

// 0 CHECKCAST \[Ljava/lang/Integer

// 0 intValue
// 0 valueOf