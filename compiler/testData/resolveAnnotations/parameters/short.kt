package test

annotation class Ann(
        val b1: Short,
        val b2: Short,
        val b3: Short,
        val b4: Short
)

@Ann(1, 1.toShort(), 32768.toShort(), 32768) class MyClass

// EXPECTED: @Ann(b1 = 1.toShort(), b2 = 1.toShort(), b3 = -32768.toShort(), b4 = 32768)