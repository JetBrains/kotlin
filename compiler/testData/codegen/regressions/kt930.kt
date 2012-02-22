fun testInt () : String {
    val r1 = 1 upto 4
    if(r1.end != 4 || r1.isReversed || r1.size != 4) return "int upto fail"

    val r2 = 4 upto 1
    if(r2.start != 0 || r2.size != 0) return "int negative upto fail"

    val r3 = 5 downto 0
    if(r3.start != 5 || r3.end != 0 || !r3.isReversed || r3.size != 6) return "int downto fail"

    val r4 = 5 downto 6
    if(r4.start != 0 || r4.end != 0 || !r3.isReversed || r4.size != 0) return "int negative downto fail"

    return "OK"
}

fun testByte () : String {
    val r1 = 1.toByte() upto 4.toByte()
    if(r1.end != 4.toByte() || r1.isReversed || r1.size != 4) return "byte upto fail"

    val r2 = 4.toByte() upto 1.toByte()
    if(r2.start != 0.toByte() || r2.size != 0) return "byte negative upto fail"

    val r3 = 5.toByte() downto 0.toByte()
    if(r3.start != 5.toByte() || r3.end != 0.toByte() || !r3.isReversed || r3.size != 6) return "byte downto fail"

    val r4 = 5.toByte() downto 6.toByte()
    if(r4.start != 0.toByte() || r4.end != 0.toByte() || !r3.isReversed || r4.size != 0) return "byte negative downto fail"

    return "OK"
}

fun testShort () : String {

    val r1 = 1.toShort() upto 4.toShort()
    if(r1.end != 4.toShort() || r1.isReversed || r1.size != 4) return "short upto fail"

    val r2 = 4.toShort() upto 1.toShort()
    if(r2.start != 0.toShort() || r2.size != 0) return "short negative upto fail"

    val r3 = 5.toShort() downto 0.toShort()
    if(r3.start != 5.toShort() || r3.end != 0.toShort() || !r3.isReversed || r3.size != 6) return "short downto fail"

    val r4 = 5.toShort() downto 6.toShort()
    if(r4.start != 0.toShort() || r4.end != 0.toShort() || !r3.isReversed || r4.size != 0) return "short negative downto fail"

    return "OK"
}

fun testLong () : String {

    val r1 = 1.toLong() upto 4.toLong()
    if(r1.end != 4.toLong() || r1.isReversed || r1.size != 4.toLong()) return "long upto fail"

    val r2 = 4.toLong() upto 1.toLong()
    if(r2.start != 0.toLong() || r2.size != 0.toLong()) return "short negative long fail"

    val r3 = 5.toLong() downto 0.toLong()
    if(r3.start != 5.toLong() || r3.end != 0.toLong() || !r3.isReversed || r3.size != 6.toLong()) return "long downto fail"

    val r4 = 5.toLong() downto 6.toLong()
    if(r4.start != 0.toLong() || r4.end != 0.toLong() || !r3.isReversed || r4.size != 0.toLong()) return "long negative downto fail"

    return "OK"
}

fun testChar () : String {

    val r1 = 'a' upto 'd'
    if(r1.end != 'd' || r1.isReversed || r1.size != 4) return "char upto fail"

    val r2 = 'd' upto 'a'
    if(r2.start != 0.toChar() || r2.size != 0) return "char negative long fail"

    val r3 = 'd' downto 'a'
    if(r3.start != 'd' || r3.end != 'a' || !r3.isReversed || r3.size != 4) return "char downto fail"

    val r4 = 'a' downto 'd'
    if(r4.start != 0.toChar() || r4.end != 0.toChar() || !r3.isReversed || r4.size != 0) return "char negative downto fail"

    return "OK"
}

fun box() : String {
    var r : String

    r = testInt()
    if(r != "OK") return r

    r = testByte()
    if(r != "OK") return r

    r = testShort()
    if(r != "OK") return r

    r = testLong()
    if(r != "OK") return r

   return "OK"
}