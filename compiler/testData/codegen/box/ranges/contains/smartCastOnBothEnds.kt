// WITH_STDLIB

// Fails on the old JVM backend because of KT-42017.
// IGNORE_BACKEND: JVM

fun checkDouble(a: Double?, b: Double?, c: Double): Boolean = a != null && b != null && c !in a..b
fun checkFloat(a: Float?, b: Float?, c: Float): Boolean = a != null && b != null && c !in a..b
fun checkLong(a: Long?, b: Long?, c: Long): Boolean = a != null && b != null && c !in a..b
fun checkInt(a: Int?, b: Int?, c: Int): Boolean = a != null && b != null && c !in a..b
fun checkChar(a: Char?, b: Char?, c: Char): Boolean = a != null && b != null && c !in a..b
fun checkByte(a: Byte?, b: Byte?, c: Byte): Boolean = a != null && b != null && c !in a..b
fun checkShort(a: Short?, b: Short?, c: Short): Boolean = a != null && b != null && c !in a..b
fun checkUInt(a: UInt?, b: UInt?, c: UInt): Boolean = a != null && b != null && c !in a..b
fun checkULong(a: ULong?, b: ULong?, c: ULong): Boolean = a != null && b != null && c !in a..b
fun checkUByte(a: UByte?, b: UByte?, c: UByte): Boolean = a != null && b != null && c !in a..b
fun checkUShort(a: UShort?, b: UShort?, c: UShort): Boolean = a != null && b != null && c !in a..b

fun box(): String {
    if (!checkDouble(1.0, 2.0, 0.0)) return "Fail Double"
    if (!checkFloat(1.0f, 2.0f, 0.0f)) return "Fail Float"
    if (!checkLong(1L, 2L, 0L)) return "Fail Long"
    if (!checkInt(1, 2, 0)) return "Fail Int"
    if (!checkChar('1', '2', '0')) return "Fail Char"
    if (!checkByte(1.toByte(), 2.toByte(), 0.toByte())) return "Fail Byte"
    if (!checkShort(1.toShort(), 2.toShort(), 0.toShort())) return "Fail Short"
    if (!checkUInt(1u, 2u, 0u)) return "Fail UInt"
    if (!checkULong(1UL, 2UL, 0UL)) return "Fail ULong"
    if (!checkUByte(1u, 2u, 0u)) return "Fail UByte"
    if (!checkUShort(1u, 2u, 0u)) return "Fail UShort"

    return "OK"
}
