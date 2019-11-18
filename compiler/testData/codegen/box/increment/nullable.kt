// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {
    var aByte: Byte? = 0
    var bByte: Byte = 0

    var aShort: Short? = 0
    var bShort: Short = 0

    var aInt: Int? = 0
    var bInt: Int = 0

    var aLong: Long? = 0
    var bLong: Long = 0

    var aFloat: Float? = 0.0f
    var bFloat: Float = 0.0f

    var aDouble: Double? = 0.0
    var bDouble: Double = 0.0

    if (aByte != null) aByte--
    bByte--
    if (aByte != bByte) return "Failed post-decrement Byte: $aByte != $bByte"

    if (aByte != null) aByte++
    bByte++
    if (aByte != bByte) return "Failed post-increment Byte: $aByte != $bByte"

    if (aShort != null) aShort--
    bShort--
    if (aShort != bShort) return "Failed post-decrement Short: $aShort != $bShort"

    if (aShort != null) aShort++
    bShort++
    if (aShort != bShort) return "Failed post-increment Short: $aShort != $bShort"
    
    if (aInt != null) aInt--
    bInt--
    if (aInt != bInt) return "Failed post-decrement Int: $aInt != $bInt"

    if (aInt != null) aInt++
    bInt++
    if (aInt != bInt) return "Failed post-increment Int: $aInt != $bInt"

    if (aLong != null) aLong--
    bLong--
    if (aLong != bLong) return "Failed post-decrement Long: $aLong != $bLong"

    if (aLong != null) aLong++
    bLong++
    if (aLong != bLong) return "Failed post-increment Long: $aLong != $bLong"

    if (aFloat != null) aFloat--
    bFloat--
    if (aFloat != bFloat) return "Failed post-decrement Float: $aFloat != $bFloat"

    if (aFloat != null) aFloat++
    bFloat++
    if (aFloat != bFloat) return "Failed post-increment Float: $aFloat != $bFloat"

    if (aDouble != null) aDouble--
    bDouble--
    if (aDouble != bDouble) return "Failed post-decrement Double: $aDouble != $bDouble"

    if (aDouble != null) aDouble++
    bDouble++
    if (aDouble != bDouble) return "Failed post-increment Double: $aDouble != $bDouble"

    return "OK"
}