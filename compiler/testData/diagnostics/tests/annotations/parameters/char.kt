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

@Ann('c', 99.toChar(), 'c'.toInt(), 'c'.toLong(), 'c'.toByte(), 'c'.toShort(), 'c'.toDouble(), 'c'.toFloat()) class MyClass

// EXPECTED: @Ann(b1 = \u0063 ('c'), b2 = \u0063 ('c'), b3 = 99, b4 = 99.toLong(), b5 = 99.toByte(), b6 = 99.toShort(), b7 = 99.0.toDouble(), b8 = 99.0.toFloat())