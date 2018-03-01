// !LANGUAGE: +InlineClasses

inline class UInt(val value: Int)
inline class ULong(val value: Long)

fun foo(u: UInt, f: (UInt) -> ULong): ULong = f(u)

fun takeUInt(u: UInt) {}

fun test() {
    val u = UInt(0)
    val l = foo(u) { // box unbox UInt
        takeUInt(it)

        ULong(0) // box ULong
    } // unbox ULong
}

// 1 INVOKESTATIC UInt\$Erased.box
// 1 INVOKEVIRTUAL UInt.unbox

// 1 INVOKESTATIC ULong\$Erased.box
// 1 INVOKEVIRTUAL ULong.unbox

// 0 valueOf
// 0 intValue
// 0 longValue