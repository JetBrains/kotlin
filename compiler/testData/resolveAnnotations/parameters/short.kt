package test

annotation class Ann(
        val b1: Short,
        val b2: Short,
        val b3: Short,
        val b4: Short
)

Ann(1, 1.toShort(), 32768.toShort(), 32768) class MyClass

// EXPECTED: Ann[b1 = 1.toShort(): jet.Short, b2 = 1.toShort(): jet.Short, b3 = -32768.toShort(): jet.Short, b4 = 32768.toInt(): jet.Int]