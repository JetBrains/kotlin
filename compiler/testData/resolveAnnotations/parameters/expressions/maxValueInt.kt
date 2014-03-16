package test

annotation class Ann(
        val p1: Int,
        val p2: Int,
        val p3: Long,
        val p4: Long,
        val p5: Int
)

Ann(
    p1 = java.lang.Integer.MAX_VALUE + 1,
    p2 = 1 + 1,
    p3 = java.lang.Integer.MAX_VALUE + 1,
    p4 = 1.toInt() + 1.toInt(),
    p5 = 1.toInt() + 1.toInt()
) class MyClass

// EXPECTED: Ann(p1 = -2147483648: Int, p2 = IntegerValueType(2): IntegerValueType(2), p3 = -2147483648: Int, p4 = 2: Int, p5 = 2: Int)
