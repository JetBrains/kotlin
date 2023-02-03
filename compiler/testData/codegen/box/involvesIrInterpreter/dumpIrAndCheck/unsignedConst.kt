// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: NATIVE
// WITH_STDLIB

const val byteVal: UByte = 1u
const val shortVal: UShort = 2u
const val intVal: UInt = 3u
const val longVal: ULong = 4uL

fun box(): String {
    if (byteVal != 1u.toUByte())    return "Fail 1"
    if (shortVal != 2u.toUShort())  return "Fail 2"
    if (intVal != 3u.toUInt())      return "Fail 3"
    if (longVal != 4u.toULong())    return "Fail 4"
    return "OK"
}