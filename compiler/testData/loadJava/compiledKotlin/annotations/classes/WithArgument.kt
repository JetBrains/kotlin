package test

annotation class IntAnno(val value: Int)
annotation class ShortAnno(val value: Short)
annotation class ByteAnno(val value: Byte)
annotation class LongAnno(val value: Long)
annotation class CharAnno(val value: Char)
annotation class BooleanAnno(val value: Boolean)
annotation class FloatAnno(val value: Float)
annotation class DoubleAnno(val value: Double)

IntAnno(42.toInt())
ShortAnno(42.toShort())
ByteAnno(42.toByte())
LongAnno(42.toLong())
CharAnno('A')
BooleanAnno(false)
FloatAnno(3.14.toFloat())
DoubleAnno(3.14)
class Class
