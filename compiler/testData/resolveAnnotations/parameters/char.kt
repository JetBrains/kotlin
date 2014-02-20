package test

annotation class Ann(
        val b1: Char,
        val b2: Char,
        val b3: Int,
        val b4: Long,
        val b5: Byte,
        val b6: Short,
        val b7: Double,
        val b8: Float
)

Ann('c', 99.toChar(), 'c'.toInt(), 'c'.toLong(), 'c'.toByte(), 'c'.toShort(), 'c'.toDouble(), 'c'.toFloat()) class MyClass

// EXPECTED: Ann(b1 = #99(c): Char, b2 = #99(c): Char, b3 = 99: Int, b4 = 99.toLong(): Long, b5 = 99.toByte(): Byte, b6 = 99.toShort(): Short, b7 = 99.0.toDouble(): Double, b8 = 99.0.toFloat(): Float)