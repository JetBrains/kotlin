// !LANGUAGE: +InlineClasses

inline class UInt(val u: Int)

fun <T> takeVarargs(vararg e: T) {}

fun test(u1: UInt, u2: UInt, u3: UInt?) {
    takeVarargs(u1, u2) // 2 box
    takeVarargs(u3)
    takeVarargs(u1, u3) // box
}

// 3 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue