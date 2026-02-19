// WITH_STDLIB
fun <T> T.id() = this

const val byteVal: UByte = 1u
const val shortVal: UShort = 2u
const val intVal: UInt = 3u
const val longVal: ULong = 4uL

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (byteVal.id() != 1u.toUByte())    return "Fail 1"
    if (shortVal.id() != 2u.toUShort())  return "Fail 2"
    if (intVal.id() != 3u.toUInt())      return "Fail 3"
    if (longVal.id() != 4u.toULong())    return "Fail 4"
    return "OK"
}
