// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun <T: Double> checkDouble(a: T?, b: T?, c: T): Boolean where T: Comparable<Double> = a != null && b != null && c !in a..b
fun <T: Float> checkFloat(a: T?, b: T?, c: T): Boolean where T: Comparable<Float> = a != null && b != null && c !in a..b
fun <T: Long> checkLong(a: T?, b: T?, c: T): Boolean where T: Comparable<Long> = a != null && b != null && c !in a..b
fun <T: Int> checkInt(a: T?, b: T?, c: T): Boolean where T: Comparable<Int> = a != null && b != null && c !in a..b
fun <T: Char> checkChar(a: T?, b: T?, c: T): Boolean where T: Comparable<Char> = a != null && b != null && c !in a..b
fun <T: Byte> checkByte(a: T?, b: T?, c: T): Boolean where T: Comparable<Byte> = a != null && b != null && c !in a..b
fun <T: Short> checkShort(a: T?, b: T?, c: T): Boolean where T: Comparable<Short> = a != null && b != null && c !in a..b
fun <T: UInt> checkUInt(a: T?, b: T?, c: T): Boolean where T: Comparable<UInt> = a != null && b != null && c !in a..b
fun <T: ULong> checkULong(a: T?, b: T?, c: T): Boolean where T: Comparable<ULong> = a != null && b != null && c !in a..b
fun <T: UByte> checkUByte(a: T?, b: T?, c: T): Boolean where T: Comparable<UByte> = a != null && b != null && c !in a..b
fun <T: UShort> checkUShort(a: T?, b: T?, c: T): Boolean where T: Comparable<UShort> = a != null && b != null && c !in a..b

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
