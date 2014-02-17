package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 - 1, 1 - 1, 1 - 1, 1 - 1) class MyClass

// EXPECTED: Ann(b = IntegerValueType(0): IntegerValueType(0), i = IntegerValueType(0): IntegerValueType(0), l = IntegerValueType(0): IntegerValueType(0), s = IntegerValueType(0): IntegerValueType(0))
