// !LANGUAGE: +InlineClasses

inline class UInt(private val data: Int)

fun f() {
    val unull = UInt(1) ?: null
}

// 0 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue