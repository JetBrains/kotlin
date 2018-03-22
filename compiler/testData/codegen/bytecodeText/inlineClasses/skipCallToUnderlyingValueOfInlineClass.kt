// !LANGUAGE: +InlineClasses

inline class UInt(val value: Int)

fun test(u1: UInt, u2: UInt) {
    val a = u1.value

    val b = u1.value.hashCode() // box int to call hashCode
    val c = u1.value + u2.value
}

// 0 INVOKESTATIC UInt\$Erased.getValue
// 0 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 1 valueOf
// 0 intValue