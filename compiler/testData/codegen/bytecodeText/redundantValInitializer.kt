package a

class A {
    companion object {
        const val constInt: Int = 0
        const val constByte: Byte = 0
        const val constLong: Long = 0L
        const val constShort: Short = 0
        const val constDouble: Double = 0.0
        const val constFloat: Float = 0.0f
        const val constBoolean: Boolean = false
        const val constChar: Char = '\u0000'
    }

    val myInt: Int = constInt
    val myByte: Byte = constByte
    val myLong: Long = constLong
    val myShort: Short = constShort
    val myDouble: Double = constDouble
    val myFloat: Float = constFloat
    val myBoolean: Boolean = constBoolean
    val myChar: Char = constChar

    val myString: String? = null
    val myAny: Any? = null
    val myObject: java.lang.Object? = null
    val myInteger: java.lang.Integer? = null
}

// 0 PUTFIELD
