// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

fun compareUInts(a: UInt, b: UInt) = a compareTo b
fun compareULongs(a: ULong, b: ULong) = a compareTo b


fun box(): String {
    if (compareUInts(0u, 1u) != -1) return "Fail 1"
    if (compareULongs(0u, 1u) != -1) return "Fail 2"
    return "OK"
}

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC kotlin/ULong.box-impl
// 0 INVOKESTATIC kotlin/UInt.box-impl
