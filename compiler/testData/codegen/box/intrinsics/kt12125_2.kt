fun box(): String {
    var aByte: Byte? = 0
    var bByte: Byte = 0

    if (aByte != null) aByte--
    bByte--
    if (aByte != bByte) return "Failed post-decrement Byte: $aByte != $bByte"

    if (aByte != null) aByte++
    bByte++
    if (aByte != bByte) return "Failed post-increment Byte: $aByte != $bByte"

    aByte = null

    return "OK"
}