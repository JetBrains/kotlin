package test

annotation class Ann(
        val b1: Byte,
        val b2: Byte,
        val b3: Byte,
        val b4: Byte
)

Ann(1, 1.toByte(), 128.toByte(), 128) class MyClass

// EXPECTED: Ann[b1 = 1.toByte(): jet.Byte, b2 = 1.toByte(): jet.Byte, b3 = -128.toByte(): jet.Byte, b4 = 128.toInt(): jet.Int]