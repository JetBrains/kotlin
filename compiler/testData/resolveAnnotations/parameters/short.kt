package test

annotation class Ann(
        val b1: Short,
        val b2: Short,
        val b3: Short,
        val b4: Short
)

Ann(1, 1.toShort(), 32768.toShort(), 32768) class MyClass

// EXPECTED: Ann(b1 = IntegerValueType(1): IntegerValueType(1), b2 = 1.toShort(): Short, b3 = -32768.toShort(): Short, b4 = IntegerValueType(32768): IntegerValueType(32768))