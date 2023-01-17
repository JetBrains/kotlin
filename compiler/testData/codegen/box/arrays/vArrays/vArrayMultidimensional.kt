// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ENABLE_JVM_IR_INLINER

inline fun <reified T> test3DVArray(initVal: T, otherVal: T): Boolean {
    val vArray3D = Array(2) { Array(3) { Array(4) { initVal } } }
    vArray3D[1][2][3] = otherVal
    return vArray3D[0][0][0] == initVal && vArray3D[1][2][3] == otherVal
}

@JvmInline
value class IcInt(val x: Int)

fun box(): String {

    if (!test3DVArray(false, true)) return "Fail 1"
    if (!test3DVArray(0.toByte(), 1.toByte())) return "Fail 2"
    if (!test3DVArray(0.toShort(), 1.toShort())) return "Fail 3"
    if (!test3DVArray(0, 1)) return "Fail 4"
    if (!test3DVArray(0.toLong(), 1.toLong())) return "Fail 5"
    if (!test3DVArray(0.toFloat(), 1.toFloat())) return "Fail 6"
    if (!test3DVArray(0.toDouble(), 1.toDouble())) return "Fail 7"
    if (!test3DVArray('a', 'b')) return "Fail 8"

    if (!test3DVArray(0.toUByte(), 1.toUByte())) return "Fail 9"
    if (!test3DVArray(0.toUShort(), 1.toUShort())) return "Fail 10"
    if (!test3DVArray(0.toUInt(), 1.toUInt())) return "Fail 11"
    if (!test3DVArray(0.toULong(), 1.toULong())) return "Fail 12"

    if (!test3DVArray(IcInt(0), IcInt(1))) return "Fail 13"
    if (!test3DVArray("a", "b")) return "Fail 14"
    if (!test3DVArray(0, "a")) return "Fail 15"

    return "OK"
}