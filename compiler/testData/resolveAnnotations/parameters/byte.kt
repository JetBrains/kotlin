package test

annotation class Ann(
        val b1: Byte,
        val b2: Byte,
        val b3: Byte,
        val b4: Byte
)

Ann(1, 1.toByte(), 128.toByte(), 128) class MyClass

// EXPECTED: Ann[b1 = IntegerValueType(1): IntegerValueType(1), b2 = 1.toByte(): jet.Byte, b3 = -128.toByte(): jet.Byte, b4 = IntegerValueType(128): IntegerValueType(128)]