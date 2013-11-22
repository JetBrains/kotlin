package test

annotation class Ann(
        val b1: Byte,
        val b2: Short,
        val b3: Int,
        val b4: Long,
        val b5: Double,
        val b6: Float,
        val b7: Char
)

Ann(+1, +1, +1, +1, +1.0, +1.0.toFloat(), +'c') class MyClass

// EXPECTED: Ann[b1 = 1.toByte(): jet.Byte, b2 = 1.toShort(): jet.Short, b3 = 1.toInt(): jet.Int, b4 = 1.toLong(): jet.Long, b5 = 1.0.toDouble(): jet.Double, b6 = 1.0.toFloat(): jet.Float, b7 = 99.toInt(): jet.Int]
