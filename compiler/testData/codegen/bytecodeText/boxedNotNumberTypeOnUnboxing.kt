fun test(p: Int?) {
    if (p != null) {
        val b = p.toByte() //intValue & I2B
        val s = p.toShort() //intValue & I2S
        val i = p.toInt() //intValue
        val l = p.toLong() //intValue & I2L
        val f = p.toFloat() //intValue & I2F
        val d = p.toDouble() //intValue & I2D
    }
}

fun test(p: Byte?) {
    if (p != null) {
        val b = p.toByte() //byteValue
        val s = p.toShort() //byteValue & I2S
        val i = p.toInt() //byteValue
        val l = p.toLong() //byteValue & I2L
        val f = p.toFloat() //byteValue & I2F
        val d = p.toDouble() //byteValue & I2D
    }
}


fun test(p: Char?) {
    if (p != null) {
        val b = p.toByte() //charValue & I2B
        val s = p.toShort() //charValue & I2S
        val i = p.toInt() //charValue
        val l = p.toLong() //charValue & I2L
        val f = p.toFloat() //charValue & I2F
        val d = p.toDouble() //charValue & I2D
    }
}

//6 Integer\.intValue
//6 Byte\.byteValue
//6 Character\.charValue
//2 I2B
//3 I2S
//3 I2L
//3 I2F
//3 I2D