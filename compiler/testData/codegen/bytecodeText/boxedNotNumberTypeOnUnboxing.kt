fun test(p: Int?) {
    if (p != null) {
        val a = p.toByte() //intValue & I2B
        val b = p.toShort() //intValue & I2S
        val c = p.toInt() //intValue
        val d = p.toLong() //intValue & I2L
        val e = p.toFloat() //intValue & I2F
        val f = p.toDouble() //intValue & I2D
    }
}

fun test(p: Byte?) {
    if (p != null) {
        val a = p.toByte() //byteValue
        val b = p.toShort() //byteValue & I2S
        val c = p.toInt() //byteValue
        val d = p.toLong() //byteValue & I2L
        val e = p.toFloat() //byteValue & I2F
        val f = p.toDouble() //byteValue & I2D
    }
}


fun test(p: Char?) {
    if (p != null) {
        val a = p.toByte() //charValue & I2B
        val b = p.toShort() //charValue & I2S
        val c = p.toInt() //charValue
        val d = p.toLong() //charValue & I2L
        val e = p.toFloat() //charValue & I2F
        val f = p.toDouble() //charValue & I2D
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