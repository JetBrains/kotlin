package test

annotation class AString(val value: String)
annotation class AChar(val value: Char)
annotation class AInt(val value: Int)
annotation class AByte(val value: Byte)
annotation class ALong(val value: Long)
annotation class ADouble(val value: Double)
annotation class AFloat(val value: Float)

interface Test {

    companion object {
        const val vstring: String = "Test"
        const val vchar: Char = 'c'
        const val vint: Int = 10
        const val vbyte: Byte = 11
        const val vlong: Long = 12
        const val vdouble: Double = 1.2
        const val vfloat: Float = 1.3.toFloat()
    }

}