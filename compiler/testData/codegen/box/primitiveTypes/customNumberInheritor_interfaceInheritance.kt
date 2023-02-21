// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// KT-46465
// WITH_STDLIB

interface Some {
    fun toChar(): Char
}

class MyNumber(val b: Boolean) : Number() {
    override fun toByte(): Byte {
        return toInt().toByte()
    }

    override fun toDouble(): Double {
        return toInt().toDouble()
    }

    override fun toFloat(): Float {
        return toInt().toFloat()
    }

    override fun toInt(): Int {
        return if (b) { 'O'.code } else { 'K'.code }
    }

    override fun toLong(): Long {
        return toInt().toLong()
    }

    override fun toShort(): Short {
        return toInt().toShort()
    }
}

@Suppress("DEPRECATION")
fun box(): String {
    val o = MyNumber(true)
    val k = MyNumber(false)
    return "${o.toChar()}${k.toChar()}"
}
