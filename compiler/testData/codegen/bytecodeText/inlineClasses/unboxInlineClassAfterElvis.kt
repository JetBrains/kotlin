// !LANGUAGE: +InlineClasses

inline class UInt(private val u: Int)

fun test(x: UInt?, y: UInt) {
    val a = x ?: y // unbox
    val b = x ?: x!! // unbox unbox
}

// 0 INVOKESTATIC UInt\$Erased.box
// 3 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue