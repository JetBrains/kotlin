// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(val u: Int) {
    fun member() {}
}

// FILE: test.kt

fun UInt?.extension() {}

fun test(a: Any, b: Any?) {
    if (a is UInt) {
        a.member()
    }

    if (b is UInt?) {
        b.extension()
    }
}

// @TestKt.class:
// 2 INSTANCEOF UInt
// 2 CHECKCAST UInt

// 1 INVOKEVIRTUAL UInt\.unbox
// 1 INVOKESTATIC UInt\.member

// 0 intValue