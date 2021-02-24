// WITH_RUNTIME

fun box(): String {
    if (!(1 in 0 until 10)) return "Fail 1 in"
    if (1 !in 0 until 10) return "Fail 1 !in"

    if (10 in 0 until 10) return "Fail 2 in"
    if (!(10 !in 0 until 10)) return "Fail 2 !in"

    if (!(1.toByte() in 0.toByte() until 10.toByte())) return "Fail 1 in Byte"
    if (1.toByte() !in 0.toByte() until 10.toByte()) return "Fail 1 !in Byte"

    if (10.toByte() in 0.toByte() until 10.toByte()) return "Fail 2 in Byte"
    if (!(10.toByte() !in 0.toByte() until 10.toByte())) return "Fail 2 !in Byte"

    if (!(1.toShort() in 0.toShort() until 10.toShort())) return "Fail 1 in Short"
    if (1.toShort() !in 0.toShort() until 10.toShort()) return "Fail 1 !in Short"

    if (10.toShort() in 0.toShort() until 10.toShort()) return "Fail 2 in Short"
    if (!(10.toShort() !in 0.toShort() until 10.toShort())) return "Fail 2 !in Short"

    if (!(1.toLong() in 0.toLong() until 10.toLong())) return "Fail 1 in Long"
    if (1.toLong() !in 0.toLong() until 10.toLong()) return "Fail 1 !in Long"

    if (10.toLong() in 0.toLong() until 10.toLong()) return "Fail 2 in Long"
    if (!(10.toLong() !in 0.toLong() until 10.toLong())) return "Fail 2 !in Long"

    return "OK"
}