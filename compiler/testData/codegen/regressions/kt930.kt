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
    val r1 = 1.byt upto 4.byt
    if(r1.end != 4.byt || r1.isReversed || r1.size != 4) return "byte upto fail"

    val r2 = 4.byt upto 1.byt
    if(r2.start != 0.byt || r2.size != 0) return "byte negative upto fail"

    val r3 = 5.byt downto 0.byt
    if(r3.start != 5.byt || r3.end != 0.byt || !r3.isReversed || r3.size != 6) return "byte downto fail"

    val r4 = 5.byt downto 6.byt
    if(r4.start != 0.byt || r4.end != 0.byt || !r3.isReversed || r4.size != 0) return "byte negative downto fail"

    return "OK"
}

fun testShort () : String {

    val r1 = 1.sht upto 4.sht
    if(r1.end != 4.sht || r1.isReversed || r1.size != 4) return "short upto fail"

    val r2 = 4.sht upto 1.sht
    if(r2.start != 0.sht || r2.size != 0) return "short negative upto fail"

    val r3 = 5.sht downto 0.sht
    if(r3.start != 5.sht || r3.end != 0.sht || !r3.isReversed || r3.size != 6) return "short downto fail"

    val r4 = 5.sht downto 6.sht
    if(r4.start != 0.sht || r4.end != 0.sht || !r3.isReversed || r4.size != 0) return "short negative downto fail"

    return "OK"
}

fun testLong () : String {

    val r1 = 1.lng upto 4.lng
    if(r1.end != 4.lng || r1.isReversed || r1.size != 4.lng) return "long upto fail"

    val r2 = 4.lng upto 1.lng
    if(r2.start != 0.lng || r2.size != 0.lng) return "short negative long fail"

    val r3 = 5.lng downto 0.lng
    if(r3.start != 5.lng || r3.end != 0.lng || !r3.isReversed || r3.size != 6.lng) return "long downto fail"

    val r4 = 5.lng downto 6.lng
    if(r4.start != 0.lng || r4.end != 0.lng || !r3.isReversed || r4.size != 0.lng) return "long negative downto fail"

    return "OK"
}

fun testChar () : String {

    val r1 = 'a' upto 'd'
    if(r1.end != 'd' || r1.isReversed || r1.size != 4) return "char upto fail"

    val r2 = 'd' upto 'a'
    if(r2.start != 0.chr || r2.size != 0) return "char negative long fail"

    val r3 = 'd' downto 'a'
    if(r3.start != 'd' || r3.end != 'a' || !r3.isReversed || r3.size != 4) return "char downto fail"

    val r4 = 'a' downto 'd'
    if(r4.start != 0.chr || r4.end != 0.chr || !r3.isReversed || r4.size != 0) return "char negative downto fail"

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