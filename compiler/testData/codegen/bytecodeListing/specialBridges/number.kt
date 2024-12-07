abstract class AbstractNumber : Number()

class MyNumber : Number() {
    override fun toByte(): Byte = 0
    override fun toChar(): Char = 0.toChar()
    override fun toDouble(): Double = 0.0
    override fun toFloat(): Float = 0.0f
    override fun toInt(): Int = 0
    override fun toLong(): Long = 0
    override fun toShort(): Short = 0
}
