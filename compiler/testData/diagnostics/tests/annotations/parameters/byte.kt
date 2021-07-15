package test

annotation class Ann(
        val b1: Byte,
        val b2: Byte,
        val b3: Byte,
        val b4: Byte
)

@Ann(1, 1.toByte(), 128.toByte(), 128) class MyClass

// EXPECTED: @Ann(b1 = 1.toByte(), b2 = 1.toByte(), b3 = -128.toByte(), b4 = 128)