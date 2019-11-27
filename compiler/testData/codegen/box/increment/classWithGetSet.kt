// IGNORE_BACKEND_FIR: JVM_IR
class AByte(var value: Byte) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Byte) {
        value = newValue
    }
}

class AShort(var value: Short) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Short) {
        value = newValue
    }
}

class AInt(var value: Int) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Int) {
        value = newValue
    }
}

class ALong(var value: Long) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Long) {
        value = newValue
    }
}

class AFloat(var value: Float) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Float) {
        value = newValue
    }
}

class ADouble(var value: Double) {
    operator fun get(i: Int) = value

    operator fun set(i: Int, newValue: Double) {
        value = newValue
    }
}

fun box(): String {
    val aByte = AByte(1)
    var bByte: Byte = 1

    val aShort = AShort(1)
    var bShort: Short = 1

    val aInt = AInt(1)
    var bInt: Int = 1

    val aLong = ALong(1)
    var bLong: Long = 1

    val aFloat = AFloat(1.0f)
    var bFloat: Float = 1.0f

    val aDouble = ADouble(1.0)
    var bDouble: Double = 1.0
    
    aByte[0]++
    bByte++
    if (aByte[0] != bByte) return "Failed post-increment Byte: ${aByte[0]} != $bByte"

    aByte[0]--
    bByte--
    if (aByte[0] != bByte) return "Failed post-decrement Byte: ${aByte[0]} != $bByte"

    aShort[0]++
    bShort++
    if (aShort[0] != bShort) return "Failed post-increment Short: ${aShort[0]} != $bShort"

    aShort[0]--
    bShort--
    if (aShort[0] != bShort) return "Failed post-decrement Short: ${aShort[0]} != $bShort"

    aInt[0]++
    bInt++
    if (aInt[0] != bInt) return "Failed post-increment Int: ${aInt[0]} != $bInt"

    aInt[0]--
    bInt--
    if (aInt[0] != bInt) return "Failed post-decrement Int: ${aInt[0]} != $bInt"

    aLong[0]++
    bLong++
    if (aLong[0] != bLong) return "Failed post-increment Long: ${aLong[0]} != $bLong"

    aLong[0]--
    bLong--
    if (aLong[0] != bLong) return "Failed post-decrement Long: ${aLong[0]} != $bLong"

    aFloat[0]++
    bFloat++
    if (aFloat[0] != bFloat) return "Failed post-increment Float: ${aFloat[0]} != $bFloat"

    aFloat[0]--
    bFloat--
    if (aFloat[0] != bFloat) return "Failed post-decrement Float: ${aFloat[0]} != $bFloat"

    aDouble[0]++
    bDouble++
    if (aDouble[0] != bDouble) return "Failed post-increment Double: ${aDouble[0]} != $bDouble"

    aDouble[0]--
    bDouble--
    if (aDouble[0] != bDouble) return "Failed post-decrement Double: ${aDouble[0]} != $bDouble"
    
    return "OK"
}