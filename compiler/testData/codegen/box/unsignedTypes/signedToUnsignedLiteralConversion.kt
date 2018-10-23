// WITH_UNSIGNED
// IGNORE_BACKEND: JS_IR, JVM_IR

@file:Suppress("SIGNED_CONSTANT_CONVERTED_TO_UNSIGNED")

fun takeUByte(u: UByte) = u.toByte()
fun takeUShort(u: UShort) = u.toShort()
fun takeUInt(u: UInt) = u.toInt()
fun takeULong(u: ULong) = u.toLong()

fun box(): String {
    val b = takeUByte(200 + 55)
    if (b != (-1).toByte()) return "Fail 1: $b"

    val s = takeUShort(123)
    if (s != 123.toShort()) return "Fail 2: $s"

    val i = takeUInt(0xFFFF_FFFF)
    if (i != -1) return "Fail 3: $i"

    val l = takeULong(0xFFFF_FFFF_FFFF)
    if (l != 0xFFFF_FFFF_FFFFL) return "Fail 4: $l"

    return "OK"
}
