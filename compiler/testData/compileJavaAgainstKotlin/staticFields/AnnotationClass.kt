package test

annotation class AString(val value: String)
annotation class AChar(val value: Char)
annotation class AInt(val value: Int)
annotation class AByte(val value: Byte)
annotation class ALong(val value: Long)
annotation class ADouble(val value: Double)
annotation class AFloat(val value: Float)

class Test {

    default object {
        val vstring: String = "Test"
        val vchar: Char = 'c'
        val vint: Int = 10
        val vbyte: Byte = 11
        val vlong: Long = 12
        val vdouble: Double = 1.2
        val vfloat: Float = 1.3.toFloat()
    }

}