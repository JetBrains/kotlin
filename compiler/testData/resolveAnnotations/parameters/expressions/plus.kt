package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 + 1, 1 + 1, 1 + 1, 1 + 1) class MyClass

// EXPECTED: Ann(b = IntegerValueType(2): IntegerValueType(2), i = IntegerValueType(2): IntegerValueType(2), l = IntegerValueType(2): IntegerValueType(2), s = IntegerValueType(2): IntegerValueType(2))
