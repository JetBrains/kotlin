fun test(p: Int?) {
    if (p != null) {
        p.toByte() //intValue & I2B
        p.toShort() //intValue & I2S
        p.toInt() //intValue
        p.toLong() //intValue & I2L
        p.toFloat() //intValue & I2F
        p.toDouble() //intValue & I2D
    }
}

fun test(p: Byte?) {
    if (p != null) {
        p.toByte() //byteValue
        p.toShort() //byteValue & I2S
        p.toInt() //byteValue
        p.toLong() //byteValue & I2L
        p.toFloat() //byteValue & I2F
        p.toDouble() //byteValue & I2D
    }
}


fun test(p: Char?) {
    if (p != null) {
        p.toByte() //charValue & I2B
        p.toShort() //charValue & I2S
        p.toInt() //charValue
        p.toLong() //charValue & I2L
        p.toFloat() //charValue & I2F
        p.toDouble() //charValue & I2D
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