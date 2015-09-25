package test

annotation class Ann(
        val b1: Byte,
        val b2: Short,
        val b3: Int,
        val b4: Long,
        val b5: Double,
        val b6: Float
)

@Ann(+1, +1, +1, +1, +1.0, +1.0.toFloat()) class MyClass

// EXPECTED: @Ann(b1 = 1.toByte(), b2 = 1.toShort(), b3 = 1, b4 = 1.toLong(), b5 = 1.0.toDouble(), b6 = 1.0.toFloat())
