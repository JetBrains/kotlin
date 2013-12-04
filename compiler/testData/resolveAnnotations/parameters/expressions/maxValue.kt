package test

annotation class Ann(
        val p1: Byte,
        val p2: Short,
        val p3: Int,
        val p4: Int,
        val p5: Long,
        val p6: Long
)

Ann(
    p1 = java.lang.Byte.MAX_VALUE + 1,
    p2 = java.lang.Short.MAX_VALUE + 1,
    p3 = java.lang.Integer.MAX_VALUE + 1,
    p4 = java.lang.Integer.MAX_VALUE + 1,
    p5 = java.lang.Integer.MAX_VALUE + 1.toLong(),
    p6 = java.lang.Long.MAX_VALUE + 1
) class MyClass

// EXPECTED: Ann[p1 = IntegerValueType(128): IntegerValueType(128), p2 = IntegerValueType(32768): IntegerValueType(32768), p3 = IntegerValueType(-2147483648): IntegerValueType(-2147483648), p4 = IntegerValueType(-2147483648): IntegerValueType(-2147483648), p5 = 2147483648.toLong(): jet.Long, p6 = IntegerValueType(-9223372036854775808): IntegerValueType(-9223372036854775808)]
