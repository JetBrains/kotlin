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
    val r1 = 1.byte upto 4.byte
    if(r1.end != 4.byte || r1.isReversed || r1.size != 4) return "byte upto fail"

    val r2 = 4.byte upto 1.byte
    if(r2.start != 0.byte || r2.size != 0) return "byte negative upto fail"

    val r3 = 5.byte downto 0.byte
    if(r3.start != 5.byte || r3.end != 0.byte || !r3.isReversed || r3.size != 6) return "byte downto fail"

    val r4 = 5.byte downto 6.byte
    if(r4.start != 0.byte || r4.end != 0.byte || !r3.isReversed || r4.size != 0) return "byte negative downto fail"

    return "OK"
}

fun testShort () : String {

    val r1 = 1.short upto 4.short
    if(r1.end != 4.short || r1.isReversed || r1.size != 4) return "short upto fail"

    val r2 = 4.short upto 1.short
    if(r2.start != 0.short || r2.size != 0) return "short negative upto fail"

    val r3 = 5.short downto 0.short
    if(r3.start != 5.short || r3.end != 0.short || !r3.isReversed || r3.size != 6) return "short downto fail"

    val r4 = 5.short downto 6.short
    if(r4.start != 0.short || r4.end != 0.short || !r3.isReversed || r4.size != 0) return "short negative downto fail"

    return "OK"
}

fun testLong () : String {

    val r1 = 1.long upto 4.long
    if(r1.end != 4.long || r1.isReversed || r1.size != 4.long) return "long upto fail"

    val r2 = 4.long upto 1.long
    if(r2.start != 0.long || r2.size != 0.long) return "short negative long fail"

    val r3 = 5.long downto 0.long
    if(r3.start != 5.long || r3.end != 0.long || !r3.isReversed || r3.size != 6.long) return "long downto fail"

    val r4 = 5.long downto 6.long
    if(r4.start != 0.long || r4.end != 0.long || !r3.isReversed || r4.size != 0.long) return "long negative downto fail"

    return "OK"
}

fun testChar () : String {

    val r1 = 'a' upto 'd'
    if(r1.end != 'd' || r1.isReversed || r1.size != 4) return "char upto fail"

    val r2 = 'd' upto 'a'
    if(r2.start != 0.char || r2.size != 0) return "char negative long fail"

    val r3 = 'd' downto 'a'
    if(r3.start != 'd' || r3.end != 'a' || !r3.isReversed || r3.size != 4) return "char downto fail"

    val r4 = 'a' downto 'd'
    if(r4.start != 0.char || r4.end != 0.char || !r3.isReversed || r4.size != 0) return "char negative downto fail"

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