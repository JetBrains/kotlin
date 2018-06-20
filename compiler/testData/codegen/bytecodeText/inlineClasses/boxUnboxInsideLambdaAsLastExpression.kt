// !LANGUAGE: +InlineClasses

inline class UInt(private val u: Int)

fun test(x: UInt?, y: UInt) {
    val a = run {
        x!!
    }

    val b = run {
        y
    }
}

// 0 INVOKESTATIC UInt\$Erased.box
// 1 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue