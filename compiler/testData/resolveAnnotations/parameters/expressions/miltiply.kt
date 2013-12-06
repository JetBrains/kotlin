package test

annotation class Ann(
        val b: Byte,
        val s: Short,
        val i: Int,
        val l: Long
)

Ann(1 * 1, 1 * 1, 1 * 1, 1 * 1) class MyClass

// EXPECTED: Ann[b = IntegerValueType(1): IntegerValueType(1), i = IntegerValueType(1): IntegerValueType(1), l = IntegerValueType(1): IntegerValueType(1), s = IntegerValueType(1): IntegerValueType(1)]
