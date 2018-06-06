// !LANGUAGE: +InlineClasses

inline class UInt(val u: Int) {
    fun member() {}
}

fun UInt?.extension() {}

fun test(a: Any, b: Any?) {
    if (a is UInt) {
        a.member()
    }

    if (b is UInt?) {
        b.extension()
    }
}

// 2 INSTANCEOF UInt
// 2 CHECKCAST UInt

// 1 INVOKEVIRTUAL UInt.unbox
// 2 INVOKESTATIC UInt\$Erased.member

// 0 intValue