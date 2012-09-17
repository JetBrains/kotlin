fun box(): String {
    if (239.toByte().toString() != (239.toByte() as Byte?).toString()) return "byte failed"
    if (239.toShort().toString() != (239.toShort() as Short?).toString()) return "short failed"
    if (239.toInt().toString() != (239.toInt() as Int?).toString()) return "int failed"
    if (239.toLong().toString() != (239.toLong() as Long?).toString()) return "long failed"
    if (239.toDouble().toString() != (239.toDouble() as Double?).toString()) return "double failed"
    if (true.toString() != (true as Boolean?).toString()) return "boolean failed"
    if ('a'.toChar().toString() != ('a'.toChar() as Char?).toString()) return "char failed"
    
    return "OK"
}